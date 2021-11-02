@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package org.kalasim.monitors

import org.kalasim.SimulationEntity
import org.koin.core.Koin
import org.kalasim.misc.DependencyContext


// See https://commons.apache.org/proper/commons-math/userguide/stat.html


abstract class Monitor<T>(
    name: String? = null,
    koin: Koin = DependencyContext.get()
) : SimulationEntity(name, koin) {

    /** Disable or enable data collection in a monitor. */
    var enabled: Boolean = true
        protected set

    fun disable() {
        enabled = false
    }


    fun <T> ifEnabled(query: () -> T): T {
        if (!enabled) {
            throw  IllegalArgumentException("can not query disabled monitor")
        }

        return query()
    }
}


interface StatisticMonitor<T> {

    /** Resets the monitor. This will also reenable it as a side-effect. */
    fun reset()

    fun addValue(value: T)
}



internal val NUM_HIST_BINS = 10


//internal data class NLMStatsData(val values: List<Double>, val timepoints: List<Double>, val durations: DoubleArray) {
//    fun plotData(): List<Pair<Double, Double>> =
//        (this.timepoints + (timepoints.last() + durations.last())).zip(values.toList() + values.last())
//}



