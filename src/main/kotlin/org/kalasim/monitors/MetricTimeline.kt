package org.kalasim.monitors

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.kalasim.*
import org.kalasim.analysis.snapshot.MetricTimelineSnapshot
import org.kalasim.misc.asCMPairList
import org.kalasim.misc.buildHistogram
import org.kalasim.misc.printHistogram
import org.kalasim.misc.printThis
import org.kalasim.misc.time.sumOf
import java.util.*
import kotlin.time.Duration


/**
 * Allows tracking a numeric quantity over time.
 *
 * @param initialValue initial value for a level timeline. It is important to set the value correctly. Default: 0
 */
//todo invert argument order
open class MetricTimeline<V : Number>(
    name: String? = null,
    internal val initialValue: V,
    envProvider: EnvProvider = DefaultProvider(),
) : Monitor<V>(name, envProvider), ValueTimeline<V> {

    val timestamps = mutableListOf<SimTime>()
    val values = ifEnabled { mutableListOf<V>() }

    var fixedEnd : SimTime? = null

    init {
        addValue(initialValue)
    }


    override fun addValue(value: V) {
        if(!enabled) return

        timestamps.add(getCurrentTime())
        values.add(value)
    }


    override fun get(time: SimTime): V {
        require(enabled) {
            "timeline '$name' is disabled. Make sure to enable it locally,  or globally using a matching tracking-policy." +
                    " For details see https://www.kalasim.org/advanced/#continuous-simulation"

        }
        require(timestamps.first() <= time) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        return timestamps.zip(values.toList()).reversed().first { it.first <= time }.second
    }

    override operator fun get(time: Number): V {
        val tick2wallTime = get<Environment>().tick2wallTime(TickTime(time.toDouble()))
        return get(tick2wallTime)
    }

    override fun total(value: V): Duration? = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.sumOf { it.second }
    }

    @Suppress("UNCHECKED_CAST")
    override fun statisticsSummary() = statsData().statisticalSummary()

    fun statsData(excludeZeros: Boolean = false): LevelStatsData<V> {
        @Suppress("DuplicatedCode")
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + getCurrentTime()
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return if(excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second.toDouble() > 0 }.unzip()
            val (timestampsFilt, _) = timestamps.zip(valuesLst).filter { it.second.toDouble() > 0 }.unzip()

            LevelStatsData(valFilt, timestampsFilt, durFilt)
        } else {
            LevelStatsData(valuesLst, timestamps, durations)
        }
    }

    private fun getCurrentTime() = fixedEnd?: env.now

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
    override fun clearHistory(before: SimTime) {
        val startFromIdx = timestamps.withIndex().firstOrNull { before > it.value }?.index ?: return

        val newTime = timestamps.subList(startFromIdx, timestamps.size)
        val newValues = values.subList(startFromIdx, values.size)

        timestamps.apply { clear(); addAll(newTime) }
        values.apply { clear(); addAll(newValues) }
    }

    fun asDoubleTimeline() = MetricTimeline(name, initialValue.toDouble(), envProvider = envProvider).apply {
        timestamps.apply { clear(); addAll(this@MetricTimeline.timestamps) }
        values.apply { clear(); addAll(this@MetricTimeline.values.map { it.toDouble() }) }
    }

    /**
     * Creates a new MetricTimeline containing only the data within the specified time range.
     *
     * @param start The start time of the clip range. Defaults to env.startDate.
     * @param end The end time of the clip range. Defaults to env.now.
     * @return A new MetricTimeline with data clipped to the specified range.
     */
    fun clip(start: SimTime = env.startDate, end: SimTime = env.now): MetricTimeline<V> {
        require(start <= end) { "start time must be less than or equal to end time" }

        val clipped = MetricTimeline(name, initialValue, envProvider = envProvider).apply {
            timestamps.clear()
            values.clear()
        }

        // Find indices within the range
        val indices = timestamps.indices.filter { i ->
            timestamps[i] >= start && timestamps[i] <= end
        }

        if (indices.isEmpty()) {
            // If no timestamps in range, add a single value at start time
            clipped.timestamps.add(start)
            clipped.values.add(get(start))
        } else {
            // Add value at start if first timestamp is after start
            if (timestamps[indices.first()] > start) {
                clipped.timestamps.add(start)
                clipped.values.add(get(start))
            }

            // Add all timestamps and values in range
            indices.forEach { i ->
                clipped.timestamps.add(timestamps[i])
                clipped.values.add(values[i])
            }

            // Add value at end if last timestamp is before end
            if (timestamps[indices.last()] < end) {
                clipped.timestamps.add(end)
                clipped.values.add(get(end))
            }
        }

        clipped.fixedEnd= end

        return clipped
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

        val colData: Map<Double, Duration> =
            statistics().data.run {
                durations.zip(values)
                    .groupBy { (_, value) -> value }
                    .map { kv -> kv.key.toDouble() to kv.value.sumOf { it.first } }
            }.toMap()

        // inherently wrong because does not take into account durations
//            val byValueHist = freq
//                .valuesIterator().iterator().asSequence().toList()
//                .map { it to freq.getCount(it) }

        colData.mapValues { env.asTicks(it.value) }.printConsole(sortByWeight = sortByWeight)
    } else {

        // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
        val hist: List<Pair<Double, Duration>> = statistics().data.run {
            val aggregatedMonitor: List<Pair<Double, Duration>> =
                durations.zip(values).groupBy { (_, value) -> value }
                    .map { kv -> kv.key.toDouble() to kv.value.sumOf { it.first } }

            aggregatedMonitor
        }

        val pmf = hist.map { it.first to it.second.inWholeSeconds.toDouble() }.asCMPairList()

        val stats =
            DescriptiveStatistics(
                EnumeratedDistribution(pmf).sample(1000, arrayOf<Double>()).toDoubleArray()
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

//
// pairwise operations
//

operator fun <V : Number> MetricTimeline<V>.plus(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Plus)

operator fun <V : Number> MetricTimeline<V>.minus(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Minus)

operator fun <V : Number> MetricTimeline<V>.times(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Times)

operator fun <V : Number> MetricTimeline<V>.div(other: MetricTimeline<V>) =
    combineInternal(this, other, ArithmeticOp.Div)

// scalar operations

operator fun <V : Number> MetricTimeline<V>.plus(factor: Number) = applyScalarFactor(factor, ArithmeticOp.Plus)
operator fun <V : Number> MetricTimeline<V>.minus(factor: Number) = applyScalarFactor(factor, ArithmeticOp.Minus)
operator fun <V : Number> MetricTimeline<V>.times(factor: Number) = applyScalarFactor(factor, ArithmeticOp.Times)
operator fun <V : Number> MetricTimeline<V>.div(factor: Number) = applyScalarFactor(factor, ArithmeticOp.Div)

private fun <V : Number> MetricTimeline<V>.applyScalarFactor(
    factor: Number,
    arithmeticOp: ArithmeticOp
): MetricTimeline<Double> {
    val constMT = MetricTimeline("Constant $factor", initialValue = factor.toDouble())

    constMT.timestamps.apply { clear(); add(this@applyScalarFactor.timestamps.first()) }

    return combineInternal(asDoubleTimeline(), constMT, arithmeticOp)
}

private fun <V : Number> combineInternal(
    mt: MetricTimeline<V>,
    other: MetricTimeline<V>,
    mode: ArithmeticOp
): MetricTimeline<Double> {
    // also see
    // https://www.geeksforgeeks.org/merge-two-sorted-linked-lists/
    // https://stackoverflow.com/questions/1774256/java-code-review-merge-sorted-lists-into-a-single-sorted-list
    val joinTime = TreeSet<SimTime>().apply { addAll(mt.timestamps); addAll(other.timestamps); add(mt.now) }

    val minTime = maxOf(mt.timestamps.first(), other.timestamps.first())
//    val maxTime = Math.min(mt.timestamps.last(), other.timestamps.last())
    val maxTime = maxOf(mt.timestamps.last(), other.timestamps.last())
//    val timeRange = minTime..maxTime

    val merged = MetricTimeline(
        "'${mt.name}' $mode '${other.name}'",
        envProvider = mt.envProvider,
        initialValue = 0.0
    ).apply {
        timestamps.clear()
        values.clear()
    }

    joinTime
        .dropWhile { it < minTime }
        .takeWhile { it <= maxTime }
        .forEach { time ->
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

//
// reduce and other set operations
//

/**
 * Calculates the sum of a list of MetricTimeline objects.
 *
 * @return The sum of the MetricTimeline objects as a new MetricTimeline<Double> object.
 */
fun <V : Number> List<MetricTimeline<V>>.sum(): MetricTimeline<Double> =
    map { it.asDoubleTimeline() }.reduce { acc, mt -> acc + mt }

fun <V : Number> List<MetricTimeline<V>>.mean(): MetricTimeline<Double> = sum() / size.toDouble()


// not pretty but allows assigning new names to merged timelines without make SimEntity.name var.
internal fun <V : Number> MetricTimeline<V>.copy(name: String = this.name): MetricTimeline<V> =
    MetricTimeline<V>(name, initialValue = initialValue,         envProvider = envProvider).apply {
        values.clear()
        values.addAll(this@copy.values)
        timestamps.clear()
        timestamps.addAll(this@copy.timestamps)
    }
