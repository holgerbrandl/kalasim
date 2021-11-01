package org.kalasim

import org.kalasim.misc.KalasimContext
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.plot.letsplot.display
import org.koin.core.Koin
import kotlin.math.round

typealias process = SequenceScope<Component>

/** Allows introspection of time-complexity of the underlying computations. The user may want to use the built-in env.tickMetrics monitor to analyze how much time is spent per time unit (aka tick).
 *
 * https://www.kalasim.org/advanced/#operational-control */
class TickMetrics(
    val sampleTicks: Double = 1.0,
    val enableMonitor: Boolean = true,
    val enableMetricEvents: Boolean = false,
    koin: Koin? = null
) : Component(koin = koin ?: KalasimContext.get()) {

    val monitor =  NumericLevelMonitor(name)

    override fun process() = sequence {
//        hold(ceil(now.value))

        while(true) {
            val before = System.currentTimeMillis()
            hold(sampleTicks)
            val after = System.currentTimeMillis()

            val tickDuration = round((after - before).toDouble()/sampleTicks).toInt()

            if(enableMetricEvents) {
                log(MetricEvent(now, tickDuration))
            }

            if(enableMonitor){
                monitor.addValue(tickDuration)
            }
        }
    }
}

class MetricEvent(tickTime: TickTime, val tickWallDurationMs: Int) : Event(tickTime)

// can this be enabled with multiple receivers in kotlin 1.6
//internal fun SequenceScope<Component>.benchmark(block: SequenceScope<Component>.() -> Any): Long {
//    val before = System.currentTimeMillis()
//    block()
//    val after = System.currentTimeMillis()
//    return after - before
//}
