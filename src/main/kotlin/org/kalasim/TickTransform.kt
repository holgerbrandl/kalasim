package org.kalasim

import org.kalasim.misc.TRACE_DF
import org.koin.core.component.KoinComponent
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.time.*

// note: remove @JvmInline because it did not seem ready on the java-interop-side
// value class ? --> Blocked by https://github.com/holgerbrandl/kalasim/issues/45
data class TickTime(val value: Double) : Comparable<TickTime> {
    override operator fun compareTo(other: TickTime): Int = value.compareTo(other.value)
    operator fun compareTo(other: Int): Int = value.compareTo(other)
    operator fun compareTo(other: Double): Int = value.compareTo(other)
    operator fun compareTo(other: Number): Int = value.compareTo(other.toDouble())

    operator fun plus(duration: Number): TickTime = TickTime(value + duration.toDouble())

    operator fun minus(other: TickTime): Double = value - other.value
    operator fun minus(duration: Double): TickTime = TickTime(value - duration)

    constructor(instant: Number) : this(instant.toDouble())

    override fun toString(): String {
        return if(value.isInfinite()) "INF" else TRACE_DF.format(value)
    }
}

/* A simple type wrapper around a duration in sim time coordinates. Not used in core API of kalasim. */
@JvmInline
value class Ticks(val value: Double) {
    constructor(instant: Number) : this(instant.toDouble())
}

val Number.tickTime: TickTime
    get() = TickTime(this)

val Number.tt: TickTime
    get() = TickTime(this)

fun Number.asTickTime() = TickTime(this)


// https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
open class TickTransform(val tickUnit: TimeUnit) {
    open fun tick2wallTime(tickTime: TickTime): Instant {
        throw IllegalArgumentException("Only supported when using OffsetTransform")
    }

    open fun wall2TickTime(instant: Instant): TickTime {
        throw IllegalArgumentException("Only supported when using OffsetTransform")
    }

     fun durationAsTicks(duration: Duration): Double = when(tickUnit) {
        TimeUnit.NANOSECONDS -> duration.toJavaDuration().toNanos()
        TimeUnit.MICROSECONDS -> duration.toJavaDuration().toNanos() / 1000.0
        TimeUnit.MILLISECONDS -> duration.toJavaDuration().toNanos() / 1000000.0
        // https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.SECONDS -> duration.toJavaDuration().toMillis() / 1000.0
        TimeUnit.MINUTES -> duration.toJavaDuration().toMillis() / 60000.0
        //https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.HOURS -> duration.toJavaDuration().seconds / 3600.0
        TimeUnit.DAYS -> duration.toJavaDuration().toMinutes() / 1440.0
    }.toDouble()
}

class OffsetTransform(val offset: Instant = Instant.now(), tickUnit: TimeUnit) : TickTransform(tickUnit) {
    override fun tick2wallTime(tickTime: TickTime): Instant {
        val ttValue = tickTime.value
        val durationSinceOffset = when(tickUnit) {
            TimeUnit.NANOSECONDS -> java.time.Duration.ofNanos(ttValue.toLong())
            TimeUnit.MICROSECONDS -> java.time.Duration.ofNanos((ttValue * 1000).toLong())
            TimeUnit.MILLISECONDS -> java.time.Duration.ofNanos((ttValue * 1000000).toLong())
            TimeUnit.SECONDS -> java.time.Duration.ofMillis((ttValue * 1000).toLong())
            TimeUnit.MINUTES -> java.time.Duration.ofMillis((ttValue * 60000).toLong())
            TimeUnit.HOURS -> java.time.Duration.ofSeconds((ttValue * 3600).toLong())
            TimeUnit.DAYS -> java.time.Duration.ofMinutes((ttValue * 1440).toLong())
        }

        return offset + durationSinceOffset
    }

    override fun wall2TickTime(instant: Instant): TickTime {
        val offsetDuration = java.time.Duration.between(offset, instant)

        return TickTime(durationAsTicks(offsetDuration.toKotlinDuration()))
    }
}

internal const val MISSING_TICK_TRAFO_ERROR = "Tick transformation not configured."


interface SimContext : KoinComponent {

    val env: Environment

    var tickTransform: TickTransform?

    /** Transforms a wall `duration` into the corresponding amount of ticks.*/
    fun Duration.asTicks(): Double {
        require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
        return tickTransform!!.durationAsTicks(this)
    }

    val Duration.ticks: Double
        get() = asTicks()

    // Scoped extensions
    fun Instant.asTickTime(): TickTime {
        require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
        return tickTransform!!.wall2TickTime(this)
    }

    operator fun TickTime.plus(duration: Duration): TickTime = TickTime(value + duration.asTicks())
    operator fun TickTime.minus(duration: Duration): TickTime = TickTime(value - duration.asTicks())

    /** Transforms a simulation time (typically `now`) to the corresponding wall time. */
    fun TickTime.asWallTime(): Instant {
        require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
        return tickTransform!!.tick2wallTime(this)
    }
}

// also provide tranforms via direct environment extensions for use outside of simulation context. *?

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asTicks(duration: Duration): Double = duration.asTicks()

/** Transforms an wall `Instant` to simulation time.*/
fun Environment.asTickTime(instant: Instant) = instant.asTickTime()


/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallTime(time: TickTime) = time.asWallTime()
fun Environment.asWallTimeOrNull(time: TickTime) = time.asWallTime()

operator fun Instant.plus(duration: kotlin.time.Duration): Instant = this + duration.toJavaDuration()
