package org.kalasim.monitors

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.kalasim.*
import org.kalasim.analysis.snapshot.MetricTimelineSnapshot
import org.kalasim.misc.*
import org.koin.core.Koin
import java.util.*

/**
 * Allows to track a numeric quantity over time.
 *
 * @param initialValue initial value for a level timeline. It is important to set the value correctly. Default: 0
 */
open class MetricTimeline<V : Number>(
    name: String? = null,
    internal val initialValue: V,
    koin: Koin = DependencyContext.get()
) : Monitor<V>(name, koin), ValueTimeline<V> {

    internal val timestamps = mutableListOf<TickTime>()
    internal val values = ifEnabled { mutableListOf<V>() }

    init {
        addValue(initialValue)
    }


    override fun addValue(value: V) {
        if(!enabled) return

        timestamps.add(env.now)
        values.add(value)
    }


    override fun get(time: Number): V {
        require(timestamps.first() <= time) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        return timestamps.zip(values.toList()).reversed().first { it.first <= time.toDouble() }.second
    }

    operator fun get(time: TickTime) = get(time.value)

    override fun total(value: V): Double = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.sumOf { it.second }
    }

    @Suppress("UNCHECKED_CAST")
    override fun statisticsSummary() = statsData().statisticalSummary()

    fun statsData(excludeZeros: Boolean = false): LevelStatsData<V> {
        @Suppress("DuplicatedCode")
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return if(excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second.toDouble() > 0 }.unzip()
            val (timestampsFilt, _) = timestamps.zip(valuesLst).filter { it.second.toDouble() > 0 }.unzip()

            LevelStatsData(valFilt, timestampsFilt, durFilt)
        } else {
            LevelStatsData(valuesLst, timestamps, durations)
        }
    }

    /** Returns the step function of this monitored value along the time axis. */
    override fun stepFun() = statsData().stepFun()

    fun statistics(excludeZeros: Boolean = false) = MetricTimelineSnapshot(this, excludeZeros)

    override val snapshot
        get() = statistics(false)

    override fun reset(initial: V) {
        require(enabled) { "resetting a disabled timeline is unlikely to have meaningful semantics" }

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

    override fun resetToCurrent() = reset(get(now))

    @Suppress("DuplicatedCode")
    override fun clearHistory(before: TickTime) {
        val startFromIdx = timestamps.withIndex().firstOrNull { before > it.value }?.index ?: return

        for(i in 0 until startFromIdx) {
            val newTime = timestamps.subList(0, startFromIdx)
            val newValues = values.subList(0, startFromIdx)

            timestamps.apply { clear(); addAll(newTime) }
            values.apply { clear(); addAll(newValues) }
        }
    }

    fun asDoubleTimeline() = MetricTimeline(name, initialValue.toDouble(), koin = getKoin()).apply {
        timestamps.apply { clear(); addAll(this@MetricTimeline.timestamps) }
        values.apply { clear(); addAll(this@MetricTimeline.values.map { it.toDouble() }) }
    }
}

fun <V : Number> MetricTimeline<V>.printHistogram(
    sortByWeight: Boolean = false,
    binCount: Int = NUM_HIST_BINS,
    valueBins: Boolean = false
) {
    println("Summary of: '${name}'")
    statistics().toJson().printThis()

    println("Histogram of: '${name}'")

    if(valueBins) {
        val freq = Frequency()

        values.forEach { freq.addValue(it as Comparable<*>) }

        val colData: Map<Double, Double> =
            statistics().data.run {
                durations.zip(values)
                    .groupBy { (_, value) -> value }
                    .map { kv -> kv.key.toDouble() to kv.value.sumOf { it.first } }
            }.toMap()

        // inherently wrong because does not take into account durations
//            val byValueHist = freq
//                .valuesIterator().iterator().asSequence().toList()
//                .map { it to freq.getCount(it) }

        colData.printConsole(sortByWeight = sortByWeight)
    } else {

        // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
        val hist: List<Pair<Double, Double>> = statistics().data.run {
            val aggregatedMonitor: List<Pair<Double, Double>> =
                durations.zip(values).groupBy { (_, value) -> value }
                    .map { kv -> kv.key.toDouble() to kv.value.sumOf { it.first } }

            aggregatedMonitor
        }

        val stats =
            DescriptiveStatistics(
                EnumeratedDistribution(hist.asCMPairList()).sample(1000, arrayOf<Double>()).toDoubleArray()
            )

        stats.buildHistogram(binCount).printHistogram()
    }
}


//
// Timeline Arithmetics
//


internal enum class ArithmeticOp {
    Plus, Minus, Times, Div;

    override fun toString() = when(this) {
        Plus -> "+"
        Minus -> "-"
        Times -> "*"
        Div -> "/"
    }
}

operator fun <V : Number> MetricTimeline<V>.plus(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Plus)

operator fun <V : Number> MetricTimeline<V>.minus(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Minus)

operator fun <V : Number> MetricTimeline<V>.times(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Times)

operator fun <V : Number> MetricTimeline<V>.div(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Div)

operator fun <V : Number> MetricTimeline<V>.div(factor: Double): MetricTimeline<Double> {
    val constMT = MetricTimeline("Constant $factor", initialValue = factor.toDouble()).apply {
        timestamps.apply { clear(); add(timestamps.first()) }
    }
    return combineInternal(this.asDoubleTimeline(), constMT, ArithmeticOp.Div)
}

fun <V : Number> List<MetricTimeline<V>>.mean(): MetricTimeline<Double> {
    val reduce = map { it.asDoubleTimeline() }.reduce { acc, mt -> acc + mt }
    return reduce / size.toDouble()
}

private fun <V : Number> combineInternal(
    mt: MetricTimeline<V>,
    other: MetricTimeline<V>,
    mode: ArithmeticOp
): MetricTimeline<Double> {
    // also see
    // https://www.geeksforgeeks.org/merge-two-sorted-linked-lists/
    // https://stackoverflow.com/questions/1774256/java-code-review-merge-sorted-lists-into-a-single-sorted-list
    val joinTime = TreeSet<TickTime>().apply { addAll(mt.timestamps); addAll(other.timestamps); add(mt.now) }

    val minTime = maxOf(mt.timestamps.first(), other.timestamps.first())
//    val maxTime = Math.min(mt.timestamps.last(), other.timestamps.last())
    val maxTime = mt.now.value
//    val timeRange = minTime..maxTime

    val merged = MetricTimeline(
        "'${mt.name}' $mode '${other.name}'",
        koin = mt.getKoin(),
        initialValue = 0.0
    ).apply {
        timestamps.clear()
        values.clear()
    }

    joinTime.dropWhile { it < minTime }.takeWhile { it <= maxTime }.forEach { time ->
        merged.timestamps.add(time)
        val value = when(mode) {
            ArithmeticOp.Plus -> mt[time].toDouble() + other[time].toDouble()
            ArithmeticOp.Minus -> mt[time].toDouble() - other[time].toDouble()
            ArithmeticOp.Times -> mt[time].toDouble() * other[time].toDouble()
            ArithmeticOp.Div -> mt[time].toDouble() / other[time].toDouble()
        }
        merged.values.add(value)
    }

    return merged
}


// not pretty but allows assigning new names to merged timelines without make SimEntity.name var.
internal fun <V : Number> MetricTimeline<V>.copy(name: String = this.name): MetricTimeline<V> =
    MetricTimeline<V>(name, initialValue = initialValue, koin = getKoin()).apply {
        values.clear()
        values.addAll(this@copy.values)
        timestamps.clear()
        timestamps.addAll(this@copy.timestamps)
    }
