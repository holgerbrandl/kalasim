package org.kalasim.monitors

import kotlinx.datetime.Instant
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.kalasim.DefaultProvider
import org.kalasim.EnvProvider
import org.kalasim.SimTime
import org.kalasim.asSimTime
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.time.sum
import org.kalasim.misc.time.sumOf
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 *
 * @sample org.kalasim.dokka.freqLevelDemo
 */
class CategoryTimeline<T>(
    val initialValue: T,
    name: String? = null,
    envProvider: EnvProvider = DefaultProvider()
) : Monitor<T>(name, envProvider), ValueTimeline<T> {

    private val timestamps = mutableListOf<SimTime>()
    private val values = ifEnabled { mutableListOf<T>() }

    init {
        reset(initialValue)
    }


    override fun reset(initial: T) {
        require(enabled) { "resetting a disabled timeline is unlikely to have meaningful semantics" }

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

    override fun addValue(value: T) {
        if (!enabled) return

        timestamps.add(env.now)
        values.add(value)
    }

    fun getPct(value: T): Double {
        val durations = xDuration()

        val freqHist = durations
            .zip(values)
            .groupBy { it.second }
            .mapValues { (_, values) ->
                values.map { it.first }.sum()
            }

        val total = freqHist.values.sum()

        return (freqHist[value] ?: error("Invalid or non-observed state")) / total
    }

    private fun xDuration(unit: DurationUnit = DurationUnit.MINUTES): DoubleArray =
        timestamps.toMutableList()
            .apply { add(env.now) }
            .zipWithNext { first, second -> second - first }
            .map { it.toDouble(unit) }
            .toDoubleArray()


    @AmbiguousDuration
    @Deprecated("use get(SimTime) instead")
    override fun get(time: Number): T? = get(env.asSimTime(time))

    override fun get(time: SimTime): T? {
        require(enabled) {
            "timeline '$name' is disabled. Make sure to enable it locally,  or globally using a matching tracking-policy." +
                    " For details see https://www.kalasim.org/advanced/#continuous-simulation"

        }
        require(timestamps.first() <= time) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        // https://youtrack.jetbrains.com/issue/KT-43776
        return timestamps.zip(values.toList()).reversed().first { it.first <= time }.second
    }

//    operator fun get(time: TickTime) = get(time.value)

    override fun total(value: T): Duration? = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations)
            .filter { it.first == value }
            .map { it.second }
            .sum()
    }

    fun printHistogram(values: List<T>? = null, sortByWeight: Boolean = false) {
        println("Summary of: '${name}'")
        println("Duration: ${env.now - timestamps[0]}")
        println("# Levels: ${this.values.distinct().size}")
        println()

        if (this.values.size <= 1) {
            println("Skipping histogram of '$name' because of to few data")
            return
        }

        //        val ed = EnumeratedDistribution(hist.asCM())
//        repeat(1000){ ed.sample()}.c

        summed()
            .map { it.key to it.value.toDouble(unit = DurationUnit.MINUTES) }
            .toMap()
            .printConsole(sortByWeight = sortByWeight, values = values)
    }


    /**
     * Calculates the distribution of timeline values within a specified time range or all available data.
     *
     * @param start The start time of the interval for which the distribution is to be computed.
     * Defaults to `null`, indicating that the distribution starts from the beginning of the timeline.
     *
     * @param end The end time of the interval for which the distribution is to be computed.
     * Defaults to `null`, indicating that the distribution extends to the current time.
     *
     * @param includeAll If `true`, includes all distinct values in the distribution even if they
     * have no associated duration in the given time range. Defaults to `false`.
     *
     * @return An `EnumeratedDistribution` instance representing the probability distribution of values
     * based on their accumulated duration within the specified time range.
     *
     * @throws IllegalArgumentException If the specified start time is before the timeline's earliest value
     * or the specified end time is after the current time.
     */
    fun valueDistribution(
        start: SimTime? = null,
        end: SimTime? = null,
        includeAll: Boolean = false
    ): EnumeratedDistribution<T> {
        val map = summed(start, end)
            .map { it.key to it.value.toDouble(DurationUnit.MINUTES) }.toMap()

//        val if(includeAll)  else this.
        val data = if (includeAll) values.distinct().associateWith { map[it]!! } else map

        return data
            .toDistribution()
    }

    /**
     * Sums the timeline duration of values within a specified time range or all available data.
     *
     * @param start The start time of the interval for which the distribution is to be computed.
     * Defaults to `null`, indicating that the distribution starts from the beginning of the timeline.
     *
     * @param end The end time of the interval for which the distribution is to be computed.
     * Defaults to `null`, indicating that the distribution extends to the current time.
     *
     * @param includeAll If `true`, includes all distinct values in the distribution even if they
     * have no associated duration in the given time range. Defaults to `false`.
     *
     * @return A map where the keys are the distinct values from the timeline and the values are
     * their summed durations within the specified time range.
     *
     * @throws IllegalArgumentException If the specified start time is before the timeline's earliest value
     * or the specified end time is after the current time.
     */
    fun summed(
        start: SimTime? = null,
        end: SimTime? = null,
    ): Map<T, Duration> {
        require(start == null || start >= timestamps.first()) { "start $start is out of timeline range [${timestamps.first()}, $now}]" }
        require(end == null || end <= now) { "end $end is out of timeline range [${timestamps.first()}, $now}]" }

        val statsData = statsData().asList(false)

        val queryInterval = (start ?: Instant.DISTANT_PAST)..(end ?: Instant.DISTANT_FUTURE)

        fun ClosedRange<Instant>.intersection(other: ClosedRange<Instant>): ClosedRange<Instant>? {
            val newStart = maxOf(this.start, other.start)
            val newEnd = minOf(this.endInclusive, other.endInclusive)
            return if (newStart <= newEnd) newStart..newEnd else null
        }

        val trimmed = statsData.map {
            val segment = it.timestamp..(it.timestamp + it.duration!!)
            val intersect = queryInterval.intersection(segment)

            if (intersect != null) {
                it.copy(timestamp = intersect.start, duration = intersect.endInclusive - intersect.start)
            } else {
                null
            }
        }.filterNotNull().filter { it.duration!! > Duration.ZERO }

        return trimmed
            .map { it.duration!! to it.value }
            .groupBy { (_, value) -> value }
            .map { kv -> kv.key to kv.value.sumOf { (it.first) } }.toMap()
    }


    override fun statisticsSummary() = statsData().statisticalSummary()

    fun statsData(): LevelStatsData<T> {
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

//        xDuration()
        return LevelStatsData(valuesLst, timestamps, durations)
    }


    /** Returns the step function of this monitored value along the time axis. */
    override fun stepFun() = statsData().stepFun()


    override fun resetToCurrent() = reset(get(now)!!)

    override fun clearHistory(before: SimTime) {
        val startFromIdx = timestamps.withIndex().firstOrNull { before > it.value }?.index ?: return

        for (i in 0 until startFromIdx) {
            val newTime = timestamps.subList(0, startFromIdx)
            val newValues = values.subList(0, startFromIdx)

            timestamps.apply { clear(); addAll(newTime) }
            values.apply { clear(); addAll(newValues) }
        }
    }


}