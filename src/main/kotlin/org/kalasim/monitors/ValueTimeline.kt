package org.kalasim.monitors

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.kalasim.asCMPairList
import org.koin.core.Koin
import org.kalasim.misc.DependencyContext


interface ValueTimeline<T> {

    /**
     * When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned.
     *
     * @throws IllegalArgumentException When querying a time before the start of the recording
     * */
    operator fun get(time: Number): T?


    /** Get the total time for which a timeline was is state `value`*/
    fun total(value: T): Double?


    /** Returns the step function of this monitored value along the time axis. */
    fun stepFun(): List<Pair<Double, T>>

    /** Resets the timeline to a new initial at the current simulation clock. This will also reenable it as a side-effect. */
    fun reset(initial: T)

    fun addValue(value: T)

    fun statisticsSummary(): EnumeratedDistribution<T>
}


fun <T> LevelStatsData<T>.statisticalSummary(): EnumeratedDistribution<T> {
    val distData = values.zip(durations).groupBy { it.first }
        .mapValues { (_, values) ->
            values.map { it.second }.sum()
        }.asCMPairList()

    return EnumeratedDistribution(distData)
}


data class LevelStatsData<T>(
    val values: List<T>,
    val timepoints: List<Double>,
    val durations: List<Double>
) {
    /** Returns the step function of time, value pairs*/
    fun stepFun(): List<Pair<Double, T>> =
        (this.timepoints + (timepoints.last() + durations.last())).zip(values.toList() + values.last())

    /**
     * @param includeNow If true a last segement with the last state (without known end or duration) will be added
     *                  at the end of the list. This is in particular helpful when visualizing these data
     */
    fun asList(includeNow: Boolean = true): List<LevelStateRecord<T>> {
        val durationsExt = if(includeNow) durations + null else durations
        return stepFun().zip(durationsExt).map { LevelStateRecord(it.first.first, it.first.second, it.second) }
    }
}

data class LevelStateRecord<T>(val timestamp: Double, val value: T, val duration: Double?)


class IntVarTimeline(initialValue: Int = 0, name: String? = null, koin: Koin = DependencyContext.get()) {
    var value: Int = initialValue
        set(value) {
            field = value
            timeline.addValue(value)
        }

    val timeline by lazy { MetricTimeline(name, koin = koin) }

    override fun toString(): String = value.toString()
}

class GenericVarTimeline<T>(initialValue: T, name: String? = null, koin: Koin = DependencyContext.get()) {
    var value: T = initialValue
        set(value) {
            field = value
            timeline.addValue(value)
        }

    val timeline by lazy { CategoryTimeline<T>(initialValue, name, koin = koin) }

    override fun toString(): String = value.toString()
}


// without wrapping type
//to inject use data class Counter(var value: Int)
//val numBalkedMonitor by lazy { MetricTimeline() }
//var numBalked: Int = 0
//    set(value) {
//        field = value
//        numBalkedMonitor.addValue(value)
//    }
