package org.kalasim.monitors

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.kalasim.asCMPairList
import org.kalasim.misc.Jsonable
import org.kalasim.roundAny
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.math.sqrt


interface LevelMonitor<T> {

    /**
     * When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned.
     * */
    operator fun get(time: Double): T?


    /** Get the total time for which a monitor was is state `value`*/
    fun total(value: T): Double?


    /** Returns the step function of this monitored value along the time axis. */
    fun stepFun(): List<Pair<Double, T>>

    /** Resets the monitor to a new initial at the current simulation clock. This will also reenable it as a side-effect. */
    fun reset(initial: T)

    fun addValue(value: T)
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
}



class IntVarMonitor(initialValue: Int = 0, name: String? = null, koin: Koin = GlobalContext.get()) {
    var value: Int = initialValue
        set(value) {
            field = value
            monitor.addValue(value)
        }

    val monitor by lazy { NumericLevelMonitor(name, koin = koin) }

    override fun toString(): String = value.toString()
}

class GenericVarMonitor<T>(initialValue: T, name: String? = null, koin: Koin = GlobalContext.get()) {
    var value: T = initialValue
        set(value) {
            field = value
            monitor.addValue(value)
        }

    val monitor by lazy { FrequencyLevelMonitor<T>(initialValue, name, koin = koin) }

    override fun toString(): String = value.toString()
}




// without wrapping type
//to inject use data class Counter(var value: Int)
//val numBalkedMonitor by lazy { NumericLevelMonitor() }
//var numBalked: Int = 0
//    set(value) {
//        field = value
//        numBalkedMonitor.addValue(value)
//    }
