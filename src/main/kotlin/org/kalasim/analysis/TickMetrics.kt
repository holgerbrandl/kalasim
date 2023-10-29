@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import org.kalasim.misc.*
import org.kalasim.monitors.MetricTimeline
import org.koin.core.Koin
import kotlin.math.round


/** Allows introspection of time-complexity of the underlying computations. The user may want to use the built-in env.tickMetrics timeline to analyze how much time is spent per time unit (aka tick).
 *
 * https://www.kalasim.org/advanced/#operational-control
 */
@OptIn(AmbiguousDurationComponent::class)
class TickMetrics(
    val sampleTicks: Double = 1.0,
    /** Enable recording of tick metrics via a `timeline` attribute of this object. */
    val enableMonitor: Boolean = true,
    /** Emit events via the kalasim message bus. */
    val enableMetricEvents: Boolean = true,
    koin: Koin? = null
) : TickedComponent(koin = koin ?: DependencyContext.get()) {

    val timeline = MetricTimeline(name, 0)

    init {
        require(env.queue.count { it is TickMetrics } == 1) {
            "tick metrics must be enabled just once"
        }
    }

    override fun process() = sequence {
//        hold(ceil(now.value))

        while(true) {
            val before = System.currentTimeMillis()
            hold(sampleTicks)
            val after = System.currentTimeMillis()

            val tickDuration = round((after - before).toDouble() / sampleTicks).toInt()

            if(enableMetricEvents) {
                log(MetricEvent(now, tickDuration))
            }

            if(enableMonitor) {
                timeline.addValue(tickDuration)
            }
        }
    }
}

class MetricEvent(simTime: SimTime, val tickWallDurationMs: Int) : Event(simTime) {
    override fun toJson() = buildJsonWithGson()
}

// can this be enabled with multiple receivers in kotlin 1.6
//internal fun SequenceScope<Component>.benchmark(block: SequenceScope<Component>.() -> Any): Long {
//    val before = System.currentTimeMillis()
//    block()
//    val after = System.currentTimeMillis()
//    return after - before
//}
