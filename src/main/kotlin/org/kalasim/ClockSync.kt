package org.kalasim

import org.kalasim.misc.ComponentTrackingConfig
import org.koin.core.Koin
import org.kalasim.misc.DependencyContext
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

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
        trackingPolicy  = ComponentTrackingConfig(false, false)
        // disable trace monitoring for clock control
//        env.traceFilters.add { it is InteractionEvent && it.curComponent is ClockSync }

        changeTickDuration(tickDuration)
    }

    private lateinit var syncConfig: SyncConfiguration

     fun changeTickDuration(tickDuration: Duration) {
        syncConfig = SyncConfiguration(
            tickMs = tickDuration.toMillis().toDouble(),
            holdTime = 1.0 / syncsPerTick.toDouble()
        )
    }

    //https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
    internal data class SyncConfiguration(val tickMs: Double, val holdTime: Double) {
        val simStart: Instant by lazy { Instant.now() }
    }


    override fun process() = sequence {

        while (true) {
            val simStart = syncConfig.simStart

            //wait until we have caught up with wall clock
            val simTimeSinceStart =
                Duration.between(simStart, simStart.plusMillis((syncConfig.tickMs * env.now.value).roundToLong()))
            val wallTimeSinceStart = Duration.between(simStart, Instant.now())

            val sleepDuration = simTimeSinceStart - wallTimeSinceStart

//            println("sim $simTimeSinceStart wall $wallTimeSinceStart")

            if (maxDelay != null && sleepDuration.abs() > maxDelay) {
                throw ClockOverloadException(
                    env.now,
                    "Maximum delay between wall clock and simulation clock exceeded at time ${env.now}. " +
                            "Difference was $sleepDuration but allowed delay was $maxDelay"
                )
            }

            // simulation is too fast if value is larger
            if (sleepDuration > Duration.ZERO) {
                // wait accordingly to let wall clock catch up
                Thread.sleep(sleepDuration.toMillis())
            }

            // wait until the next sync event is due
            hold(syncConfig.holdTime)
        }
    }
}

/**
 * Will be thrown if the maximum delay time is exceeded when using [clock synchronization](https://www.kalasim.org/advanced/#clock-synchronization).
 */
class ClockOverloadException(val simTime: TickTime, msg: String) : RuntimeException(msg)


