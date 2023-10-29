package org.kalasim

import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import kotlin.time.Duration

/**
 * Describes a component within a simulation model. Most importantly this exposes access to the simulation `org.kalasim.Environment`.
 */
interface SimContext : KoinComponent {

    /** The environment of this simulation model. */
    val env: Environment

    /** Transforms a wall `duration` into the corresponding amount of ticks.*/
    fun Duration.asTicks(): Double {
//        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.tickTransform.durationAsTicks(this)
    }

    val Duration.ticks: Double
        get() = asTicks()

    // Scoped extensions
    fun Instant.toTickTime(): TickTime {
        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.wall2TickTime(this)
    }

//    operator fun TickTime.plus(duration: Duration): TickTime = TickTime(value + duration.asTicks())
//    operator fun TickTime.minus(duration: Duration): TickTime = TickTime(value - duration.asTicks())

    /** Transforms a simulation time (typically `now`) to the corresponding wall time. */
    @Deprecated("no longer needed as sim-time is also expressed as kotlinx.datetimex.Instant starting in v0.12")
    fun TickTime.toWallTime(): Instant {
        require(env.startDate != null) { MISSING_TICK_TRAFO_ERROR }
        return env.tick2wallTime(this)
    }


    fun Number.toDuration(): Duration = env.tickTransform.ticks2Duration(this.toDouble())


    // would be nice but makes it harder to use TickTime outside of env
//    operator fun TickTime.minus(other: TickTime): Duration = (value - other.value).toDuration()

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
     * Returns a random element from this collection.
     *
     * Note: this method intentionally overwrites the same-signature method of the kotlin standard library to
     * provide better control over randomization of simulation experiments.
     */
    fun <T> Array<T>.random(): T {
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