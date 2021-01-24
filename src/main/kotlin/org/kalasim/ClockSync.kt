package org.kalasim

import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * A component that allows to synchronize wall time to simulation. See the [user manual](https://www.kalasim.org/advanced/#clock-synchronization) for an in-depth overview about clock synchronization.
 *
 * @param tickDuration The duration of a simulation tick in wall clock coordinates. Can be created with Duration.ofSeconds(1), Duration.ofMinutes(n) and so on.
 * @param speedUp To run simulations, in more than realtime, the user can specify to run a simulation faster (speedUp > 1) or slower (speedUp<1) than realtime.
 * @param syncsPerTick Defines how often a clock synchronization should happen. Per default it synchronizes once per _tick_ (i.e. an 1-increment of simulation time).
 * @param maxDelay If provided, `kalasim` will throw an `ClockOverloadException` if the specified delay is exceeded by difference of wall-time and (transformed) simulation time. Defaults to `null`.
 */
class ClockSync(
    tickDuration: Duration,
    val speedUp: Number = 1,
    syncsPerTick: Number = 1,
    val maxDelay: Duration? = null,
    koin: Koin = GlobalContext.get()
) : Component(koin = koin) {

    init {
        // disable trace monitoring for clock control
        env.traceFilters.add { it is InteractionEvent && it.curComponent is ClockSync }
    }

    val tickMs = tickDuration.toMillis().toDouble() / speedUp.toDouble()
    val holdTime = 1.0 / syncsPerTick.toDouble()

    //https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
    lateinit var simStart: Instant

    override fun process() = sequence {
//        if (!this@ClockSync::simStart.isInitialized) {
        simStart = Instant.now()
//        }

        while(true) {
            //wait until we have caught up with wall clock
            val simTimeSinceStart = Duration.between(simStart, simStart.plusMillis((tickMs * env.now).roundToLong()));
            val wallTimeSinceStart = Duration.between(simStart, Instant.now())

            val sleepDuration = simTimeSinceStart - wallTimeSinceStart

//            println("sim $simTimeSinceStart wall $wallTimeSinceStart")

            if(maxDelay != null && sleepDuration.abs() > maxDelay) {
                throw ClockOverloadException(
                    env.now,
                    "Maximum delay between wall clock and simulation clock exceeded at time ${env.now}. " +
                            "Difference was $sleepDuration but allowed delay was $maxDelay"
                )
            }

            // simulation is too fast if value is larger
            if(sleepDuration > Duration.ZERO) {
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
class ClockOverloadException(val simTime: Double, msg: String) : RuntimeException(msg)


// https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
fun interface TickTransform {
    fun transform(tickTime: Double): Instant
}


fun Environment.configureTickTransformation(transform: (Double) -> Instant) {
    tickTransform = TickTransform { transform(it) }
}

class OffsetTransform(val offset: Instant = Instant.now(), val tickUnit: TimeUnit = TimeUnit.MINUTES) : TickTransform {
    override fun transform(tickTime: Double): Instant {
        val durationSinceOffset = when(tickUnit){
            TimeUnit.NANOSECONDS -> Duration.ofNanos(tickTime.toLong())
            TimeUnit.MICROSECONDS -> Duration.ofNanos((tickTime*1000).toLong())
            TimeUnit.MILLISECONDS -> Duration.ofMillis(tickTime.toLong())
            TimeUnit.SECONDS -> Duration.ofSeconds(tickTime.toLong())
            TimeUnit.MINUTES -> Duration.ofMinutes(tickTime.toLong())
            TimeUnit.HOURS -> Duration.ofHours(tickTime.toLong())
            TimeUnit.DAYS -> Duration.ofDays(tickTime.toLong())
        }

        return offset + durationSinceOffset
    }
}