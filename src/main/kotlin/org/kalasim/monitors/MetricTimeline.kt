package org.kalasim.monitors

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.kalasim.*
import org.kalasim.asCMPairList
import org.kalasim.misc.*
import org.koin.core.Koin
import java.util.*
import kotlin.math.sqrt

/**
 * Allows to track a numeric quantity over time.
 *
 * @param initialValue initial value for a level timeline. It is important to set the value correctly. Default: 0
 */
class MetricTimeline(
    name: String? = null,
    private val initialValue: Number = 0,
    koin: Koin = DependencyContext.get()
) : Monitor<Number>(name, koin), ValueTimeline<Number> {

    internal val timestamps = mutableListOf<TickTime>()
    internal val values = ifEnabled { mutableListOf<Double>() }

    init {
        addValue(initialValue)
    }


    override fun addValue(value: Number) {
        if (!enabled) return

        timestamps.add(env.now)
        values.add(value.toDouble())
    }


    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    operator fun inc(): MetricTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt + 1))

        return this
    }

    operator fun dec(): MetricTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt - 1))

        return this
    }

    override fun get(time: Number): Number {
        require(timestamps.first() <= time) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        return timestamps.zip(values.toList()).reversed().first { it.first <= time.toDouble() }.second
    }

    operator fun get(time: TickTime) = get(time.value)

    override fun total(value: Number): Double = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.sumOf { it.second }
    }

    @Suppress("UNCHECKED_CAST")
    override fun statisticsSummary() = (statsData() as LevelStatsData<Number>).statisticalSummary()

    fun statsData(excludeZeros: Boolean = false): LevelStatsData<Double> {
        @Suppress("DuplicatedCode")
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return if (excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second > 0 }.unzip()
            val (timestampsFilt,_ ) = timestamps.zip(valuesLst).filter { it.second > 0 }.unzip()

            LevelStatsData(valFilt, timestampsFilt, durFilt)
        } else {
            LevelStatsData(valuesLst, timestamps, durations)
        }
    }

    /** Returns the step function of this monitored value along the time axis. */
    override fun stepFun() = statsData().stepFun()

    fun statistics(excludeZeros: Boolean = false) = MetricTimelineStats(this, excludeZeros)

     override val snapshot
        get() = statistics(false)

    override fun reset(initial: Number) {
        require(enabled) { "resetting a disabled timeline is unlikely to have meaningful semantics" }

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

    override fun resetToCurrent() = reset(get(now))

    @Suppress("DuplicatedCode")
    override fun clearHistory(before: TickTime) {
        val startFromIdx = timestamps.withIndex().firstOrNull { before > it.value }?.index ?: return

        for (i in 0 until startFromIdx) {
            val newTime = timestamps.subList(0, startFromIdx)
            val newValues = values.subList(0, startFromIdx)

            timestamps.apply { clear(); addAll(newTime) }
            values.apply { clear(); addAll(newValues) }
        }
    }
}

fun MetricTimeline.printHistogram(sortByWeight: Boolean = false, binCount: Int = NUM_HIST_BINS, valueBins: Boolean = false) {
    println("Summary of: '${name}'")
    statistics().toJson().printThis()

    println("Histogram of: '${name}'")

    if (valueBins) {
        val freq = Frequency()

        values.forEach { freq.addValue(it) }

        val colData: Map<Double, Double> =
            statistics().data.run {
                durations.zip(values)
                    .groupBy { (_, value) -> value }
                    .map { kv -> kv.key to kv.value.sumOf { it.first } }
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
                    .map { kv -> kv.key to kv.value.sumOf { it.first } }

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

    override fun toString() = when (this) {
        Plus -> "+"
        Minus -> "-"
        Times -> "*"
        Div -> "/"
    }
}

operator fun MetricTimeline.plus(other: MetricTimeline) = combineInternal(this, other, ArithmeticOp.Plus)
operator fun MetricTimeline.minus(other: MetricTimeline) = combineInternal(this, other, ArithmeticOp.Minus)
operator fun MetricTimeline.times(other: MetricTimeline) = combineInternal(this, other, ArithmeticOp.Times)
operator fun MetricTimeline.div(other: MetricTimeline) = combineInternal(this, other, ArithmeticOp.Div)

operator fun MetricTimeline.div(factor: Number): MetricTimeline {
    val constMT = MetricTimeline("Constant $factor", initialValue = factor).apply {
        timestamps.apply { clear(); add(timestamps.first()) }
    }
    return combineInternal(this, constMT, ArithmeticOp.Div)
}

fun List<MetricTimeline>.mean() = reduce { acc, mt -> acc + mt } / size

private fun combineInternal(mt: MetricTimeline, other: MetricTimeline, mode: ArithmeticOp): MetricTimeline {
    // also see
    // https://www.geeksforgeeks.org/merge-two-sorted-linked-lists/
    // https://stackoverflow.com/questions/1774256/java-code-review-merge-sorted-lists-into-a-single-sorted-list
    val joinTime = TreeSet<TickTime>().apply { addAll(mt.timestamps); addAll(other.timestamps); add(mt.now) }

    val minTime = maxOf(mt.timestamps.first(), other.timestamps.first())
//    val maxTime = Math.min(mt.timestamps.last(), other.timestamps.last())
    val maxTime = mt.now.value
//    val timeRange = minTime..maxTime

    val merged = MetricTimeline("'${mt.name}' $mode '${other.name}'").apply {
        timestamps.clear()
        values.clear()
    }

    joinTime.dropWhile { it < minTime }.takeWhile { it <= maxTime }.forEach { time ->
        merged.timestamps.add(time)
        val value = when (mode) {
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
internal fun MetricTimeline.copy(name: String = this.name): MetricTimeline = MetricTimeline(name).apply {
    values.clear()
    values.addAll(this@copy.values)
    timestamps.clear()
    timestamps.addAll(this@copy.timestamps)
}


//
// Statistical Summary
//

class MetricTimelineStats(nlm: MetricTimeline, excludeZeros: Boolean = false) : Jsonable(), EntitySnapshot {
    val duration: Double

    val mean: Double?
    val standardDeviation: Double?

    val min: Double?
    val max: Double?

    internal val data = nlm.statsData(excludeZeros)

//    val median :Double = TODO()
//    val ninetyfivePercentile :Double = TODO()
//    val ninetyninePercentile :Double = TODO()

    init {
        min = data.values.minOrNull()
        max = data.values.maxOrNull()

        if (data.durations.any { it != 0.0 }) {
            val durationsArray = data.durations.toDoubleArray()
            mean = Mean().evaluate(data.values.toDoubleArray(), durationsArray)
            standardDeviation = sqrt(Variance().evaluate(data.values.toDoubleArray(), durationsArray))
//            val median = Median().evaluate(data.values.toDoubleArray(), data.durations) // not supported by commons3
        } else {
            // this happens if all there is in total no duration associated once 0s are removed
            mean = null
            standardDeviation = null
        }
        // weights not supported
        // mean = Median().evaluate(data.values.toDoubleArray(), data.timepoints.toDoubleArray())

        duration = data.durations.sum()
    }

    override fun toJson() = json {
        "duration" to duration
        "mean" to mean?.roundAny()
        "standard_deviation" to standardDeviation?.roundAny().nanAsNull()
        "min" to min?.roundAny()
        "max" to max?.roundAny()
    }
}
