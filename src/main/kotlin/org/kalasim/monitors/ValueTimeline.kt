package org.kalasim.monitors

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.kalasim.SimTime
import org.kalasim.misc.asCMPairList
import kotlin.time.Duration


interface ValueTimeline<T> {

    /**
     * When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned.
     *
     * @throws IllegalArgumentException When querying a time before the start of the recording
     * */
    operator fun get(time: SimTime): T?

    operator fun get(time: Number): T?


    /** Get the total time for which a timeline was is state `value`*/
    fun total(value: T): Duration?


    /** Returns the step function of this monitored value along the time axis. */
    fun stepFun(): List<StepRecord<T>>

    /** Resets the timeline to a new initial at the current simulation clock. This will also reenable it as a side-effect. */
    fun reset(initial: T)

    fun addValue(value: T)

    fun statisticsSummary(): EnumeratedDistribution<T>

    /** Resets the timeline to its current value. Mainly needed to periodic history cleanups in long running simulations. */
    fun resetToCurrent()

    /** Discards all history before the given time. */
    fun clearHistory(before: SimTime)
}

// replacement for Pair to get better auto-conversion to data-frame
data class StepRecord<T>(val time: SimTime, val value: T)


fun <T> LevelStatsData<T>.statisticalSummary(): EnumeratedDistribution<T> {
    val distData =
        values.zip(durations).groupBy { it.first }
            .mapValues { (_, values) -> values.sumOf { it.second.inWholeSeconds.toDouble() } }
            .asCMPairList()

    return EnumeratedDistribution(distData)
}


data class LevelStatsData<T>(
    val values: List<T>,
    val timepoints: List<SimTime>,
    val durations: List<Duration>
) {
    /** Returns the step function of time, value pairs*/
    fun stepFun(): List<StepRecord<T>> {
        val tickTimes = this.timepoints + (timepoints.last() + durations.last())
        val values = values.toList() + values.last()

        return tickTimes.zip(values.asIterable()) { time, value -> StepRecord(time, value) }
    }

    /**
     * @param includeNow If true a last segement with the last state (without known end or duration) will be added
     *                  at the end of the list. This is in particular helpful when visualizing these data
     */
    fun asList(includeNow: Boolean = true): List<LevelStateRecord<T>> {
        val durationsExt = if (includeNow) durations + null else durations
        return stepFun().zip(durationsExt).map { LevelStateRecord(it.first.time, it.first.value, it.second) }
    }
}

data class LevelStateRecord<T>(val timestamp: SimTime, val value: T, val duration: Duration?)

//
//class IntVarTimeline(initialValue: Int = 0, name: String? = null, koin: Koin = DependencyContext.get()) {
//    var value: Int = initialValue
//        set(value) {
//            field = value
//            timeline.addValue(value)
//        }
//
//    val timeline by lazy { MetricTimeline<Int>(name, koin = koin) }
//
//    override fun toString(): String = value.toString()
//}

//class GenericVarTimeline<T>(initialValue: T, name: String? = null, koin: Koin = DependencyContext.get()) {
//    var value: T = initialValue
//        set(value) {
//            field = value
//            timeline.addValue(value)
//        }
//
//    val timeline by lazy { CategoryTimeline<T>(initialValue, name, koin = koin) }
//
//    override fun toString(): String = value.toString()
//}


// without wrapping type
//to inject use data class Counter(var value: Int)
//val numBalkedMonitor by lazy { MetricTimeline() }
//var numBalked: Int = 0
//    set(value) {
//        field = value
//        numBalkedMonitor.addValue(value)
//    }
