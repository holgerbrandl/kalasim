package org.kalasim

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kalasim.misc.*
import org.koin.core.Koin
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A component that allows to synchronize wall time to simulation. See the [user manual](https://www.kalasim.org/advanced/#clock-synchronization) for an in-depth overview about clock synchronization.
 *
 * @param tickDuration The duration of a simulation tick in wall clock coordinates. Can be created with Duration.ofSeconds(1), Duration.ofMinutes(n) and so on.
 * @param syncsPerTick Defines how often a clock synchronization should happen. Per default it synchronizes once per _tick_ (i.e. an 1-increment of simulation time).
 * @param maxDelay If provided, `kalasim` will throw an `ClockOverloadException` if the specified delay is exceeded by difference of wall-time and (transformed) simulation time. Defaults to `null`.
 */
class ClockSync(
    tickDuration: Duration,
    val syncsPerTick: Number = 10,
    val maxDelay: Duration? = null,
    koin: Koin = DependencyContext.get()
) : Component(
    trackingConfig = ComponentTrackingConfig(logCreation = false, logStateChangeEvents = false),
    koin = koin
) {
    // todo add secondary constructor that accepts speed-parameter instead of too complex
    /**
     * A component that allows to synchronize wall time to simulation. See the [user manual](https://www.kalasim.org/advanced/#clock-synchronization) for an in-depth overview about clock synchronization.
     *
     * @param speedUp If set to 1.0 this will run with wall-time. Setting it to 2 would be twice as fast as wall-time and so on.
     * @param syncsPerTick Defines how often a clock synchronization should happen. Per default it synchronizes once per _tick_ (i.e. an 1-increment of simulation time).
     * @param maxDelay If provided, `kalasim` will throw an `ClockOverloadException` if the specified delay is exceeded by difference of wall-time and (transformed) simulation time. Defaults to `null`.
     */
    @OptIn(AmbiguousDuration::class)
    constructor(
        speedUp: Double = 1.0,
        syncsPerTick: Number = 10,
        maxDelay: Duration? = null,
        koin: Koin = DependencyContext.get()
    ) : this(
        // compute necessary tick duration
        koin.get<Environment>().asDuration(1.0 / speedUp),
        syncsPerTick,
        maxDelay,
        koin
    )

    fun adjustSpeedUp(speedUp: Double) {
        require(speedUp > 0) { "speed up must be positive" }
        logger.info { "adjusting speed up to $speedUp" }

        tickDuration = env.asDuration(1.0 / speedUp)
    }

    var tickDuration: Duration = tickDuration
        set(value) {
            field = value

            syncStartWall = null
            syncStartSim = null
        }


    val holdTime = (1.0 / syncsPerTick.toDouble()).toDuration()

    private var syncStartWall: Instant? = null
    private var syncStartSim: SimTime? = null

    override fun process() = sequence {

        while(true) {
            //wait until we have caught up with wall clock
            val tickDurationMs = tickDuration.inWholeMilliseconds.toDouble()

            // set the start time if not yet done (happens only after changing tickDuration
            syncStartSim = syncStartSim ?: env.now
            val now = Clock.System.now()
            syncStartWall = syncStartWall ?: now


//            val simTimeSinceSyncStart = ((env.now - syncStartTicks!!) * tickDurationMs).roundToLong().milliseconds
            val simTimeSinceSyncStart =
                ((env.now - syncStartSim!!).asTicks() * tickDurationMs).roundToLong().milliseconds
            val wallTimeSinceSyncStart = now - syncStartWall!!

            val sleepDuration = simTimeSinceSyncStart - wallTimeSinceSyncStart

//            println("synchronizing will wall by sleeping for $sleepDuration")

//            if (abs(sleepDuration.toMillis()) > 5000L) {
//                println("sim $simTimeSinceSyncStart wall $wallTimeSinceSyncStart; Resync by sleep for ${sleepDuration}ms")
//            }

            if(maxDelay != null && sleepDuration > maxDelay) {
                throw ClockOverloadException(
                    env.now,
                    "Maximum delay between wall clock and simulation clock exceeded at time ${env.now}. " +
                            "Difference was $sleepDuration but allowed delay was $maxDelay"
                )
            }

            // simulation is too fast if value is larger
            if(sleepDuration > Duration.ZERO) {
                // wait accordingly to let wall clock catch up
                Thread.sleep(sleepDuration.inWholeMilliseconds)
            }

            // wait until the next sync event is due
            hold(holdTime)
        }
    }
}

/**
 * Will be thrown if the maximum delay time is exceeded when using [clock synchronization](https://www.kalasim.org/advanced/#clock-synchronization).
 */
class ClockOverloadException(val timestamp: SimTime, msg: String) : RuntimeException(msg)


