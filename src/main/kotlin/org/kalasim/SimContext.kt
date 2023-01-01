package org.kalasim

import org.koin.core.component.KoinComponent
import java.time.Instant
import kotlin.time.Duration

/**
 * Describes a component within a simulation model. Most importantly this exposes access to the simulation `org.kalasim.Environment`.
 */
interface SimContext : KoinComponent {

    /** The environment of this simulation model. */
    val env: Environment

    var tickTransform: TickTransform

    /** Transforms a wall `duration` into the corresponding amount of ticks.*/
    fun Duration.asTicks(): Double {
//        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.tickTransform.durationAsTicks(this)
    }

    val Duration.ticks: Double
        get() = asTicks()

    // Scoped extensions
    fun Instant.asTickTime(): TickTime {
        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.wall2TickTime(this)
    }

    operator fun TickTime.plus(duration: Duration): TickTime = TickTime(value + duration.asTicks())
    operator fun TickTime.minus(duration: Duration): TickTime = TickTime(value - duration.asTicks())

    /** Transforms a simulation time (typically `now`) to the corresponding wall time. */
    fun TickTime.asWallTime(): Instant {
        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.tick2wallTime(this)
    }


    /**
     * Returns a random element from this collection.
     *
     * Note: this method intentionally overwrites the same-signature method of the kotlin standard library to
     * provide better control over randomization of simulation experiments.
     */
    fun <T> Collection<T>.random(): T {
        return random(env.random)
    }

    /**
     * Returns a new list with the elements of this list randomly shuffled.
     *
     * Note: this method intentionally overwrites the same-signature method of the kotlin standard library to
     * provide better control over randomization of simulation experiments.
     */
    fun <T> Iterable<T>.shuffled(): List<T> = toMutableList().apply { shuffle(env.random) }
}