package org.kalasim

import org.kalasim.misc.ComponentTrackingConfig
import org.kalasim.misc.DependencyContext
import org.koin.core.Koin
import java.time.Duration.between
import java.time.Duration.ofMillis
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.toJavaDuration

/**
 * A component that allows to synchronize wall time to simulation. See the [user manual](https://www.kalasim.org/advanced/#clock-synchronization) for an in-depth overview about clock synchronization.
 *
 * @param tickDuration The duration of a simulation tick in wall clock coordinates. Can be created with Duration.ofSeconds(1), Duration.ofMinutes(n) and so on.
 * @param syncsPerTick Defines how often a clock synchronization should happen. Per default it synchronizes once per _tick_ (i.e. an 1-increment of simulation time).
 * @param maxDelay If provided, `kalasim` will throw an `ClockOverloadException` if the specified delay is exceeded by difference of wall-time and (transformed) simulation time. Defaults to `null`.
 */
class ClockSync(
    tickDuration: Duration,
    val syncsPerTick: Number = 1,
    val maxDelay: Duration? = null,
    koin: Koin = DependencyContext.get()
) : Component(koin = koin) {


    init {
        trackingPolicy = ComponentTrackingConfig(logCreation = false, logStateChangeEvents = false)
        // disable trace monitoring for clock control
//        env.traceFilters.add { it is InteractionEvent && it.curComponent is ClockSync }
    }

    var tickDuration: Duration = tickDuration
        set(value) {
            field = value

            syncStartWT = null
            syncStartTicks = null
        }


    private val holdTime = 1.0 / syncsPerTick.toDouble()

    private var syncStartWT: Instant? = null
    private var syncStartTicks: TickTime? = null

    override fun process() = sequence {

        while (true) {
            //wait until we have caught up with wall clock
            val tickDurationMs = tickDuration.inWholeMilliseconds.toDouble()

            // set the start time if not yet done (happens only after changing tickDuration
            syncStartTicks = syncStartTicks ?: env.now
            syncStartWT = syncStartWT ?: Instant.now()


            val simTimeSinceSyncStart = ofMillis(((env.now - syncStartTicks!!) * tickDurationMs).roundToLong())
            val wallTimeSinceSyncStart = between(syncStartWT, Instant.now())

            val sleepDuration = simTimeSinceSyncStart - wallTimeSinceSyncStart

//            if (abs(sleepDuration.toMillis()) > 5000L) {
//                println("sim $simTimeSinceSyncStart wall $wallTimeSinceSyncStart; Resync by sleep for ${sleepDuration}ms")
//            }

            if (maxDelay != null && sleepDuration.abs() > maxDelay.toJavaDuration()) {
                throw ClockOverloadException(
                    env.now,
                    "Maximum delay between wall clock and simulation clock exceeded at time ${env.now}. " +
                            "Difference was $sleepDuration but allowed delay was $maxDelay"
                )
            }

            // simulation is too fast if value is larger
            if (sleepDuration > Duration.ZERO.toJavaDuration()) {
                // wait accordingly to let wall clock catch up
                Thread.sleep(sleepDuration.toMillis())
            }

            // wait until the next sync event is due
            hold(holdTime)
        }
    }
}

/**
 * Will be thrown if the maximum delay time is exceeded when using [clock synchronization](https://www.kalasim.org/advanced/#clock-synchronization).
 */
class ClockOverloadException(val simTime: TickTime, msg: String) : RuntimeException(msg)


