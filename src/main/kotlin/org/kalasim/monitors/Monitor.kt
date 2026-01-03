package org.kalasim.monitors

import org.kalasim.DefaultProvider
import org.kalasim.EnvProvider
import org.kalasim.SimulationEntity
import org.kalasim.misc.DependencyContext
import org.koin.core.Koin


// See https://commons.apache.org/proper/commons-math/userguide/stat.html


abstract class Monitor<T>(
    name: String? = null,
     envProvider: EnvProvider = DefaultProvider()
) : SimulationEntity(name, envProvider) {

    /** Disable or enable data collection in a timeline. */
    var enabled: Boolean = true


    // is this good practise
    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun <T> ifEnabled(query: () -> T): T {
        if(!enabled) {
            throw IllegalArgumentException("can not query disabled timeline")
        }

        return query()
    }
}


interface ValueMonitor<T> {

    /** Resets the timeline. This will also re-enable it as a side-effect. */
    fun reset()

    fun addValue(value: T)
}


internal const val NUM_HIST_BINS = 10


//internal data class NLMStatsData(val values: List<Double>, val timepoints: List<Double>, val durations: DoubleArray) {
//    fun plotData(): List<Pair<Double, Double>> =
//        (this.timepoints + (timepoints.last() + durations.last())).zip(values.toList() + values.last())
//}



