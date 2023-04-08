package org.kalasim

import kotlinx.datetime.Clock
import org.kalasim.misc.TRACE_DF
import kotlinx.datetime.Instant
import kotlin.time.DurationUnit
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

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

fun Number.toTickTime() = TickTime(this)


// https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
internal open class TickTransform(val tickUnit: DurationUnit) {
    open fun ticks2Duration(ticks: Double) = ticks2Duration(Ticks(ticks))

    open fun ticks2Duration(ticks: Ticks): Duration = when(tickUnit){
        DurationUnit.NANOSECONDS -> ticks.value.nanoseconds
        DurationUnit.MICROSECONDS -> ticks.value.microseconds
        DurationUnit.MILLISECONDS -> ticks.value.milliseconds
        DurationUnit.SECONDS -> ticks.value.seconds
        DurationUnit.MINUTES -> ticks.value.minutes
        DurationUnit.HOURS -> ticks.value.hours
        DurationUnit.DAYS -> ticks.value.days
    }


    open fun tick2wallTime(tickTime: TickTime): Instant {
        throw IllegalArgumentException("Only supported when using OffsetTransform")
    }

    open fun wall2TickTime(instant: Instant): TickTime {
        throw IllegalArgumentException("Only supported when using OffsetTransform")
    }

    fun durationAsTicks(duration: Duration): Double = when(tickUnit) {
        DurationUnit.NANOSECONDS -> duration.toDouble(DurationUnit.NANOSECONDS)
        DurationUnit.MICROSECONDS -> duration.toDouble(DurationUnit.MICROSECONDS)
        DurationUnit.MILLISECONDS -> duration.toDouble(DurationUnit.MILLISECONDS)
        // https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        DurationUnit.SECONDS -> duration.toDouble(DurationUnit.MILLISECONDS)
        DurationUnit.MINUTES -> duration.toDouble(DurationUnit.MINUTES)
        //https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        DurationUnit.HOURS -> duration.toDouble(DurationUnit.HOURS)
        DurationUnit.DAYS -> duration.toDouble(DurationUnit.DAYS)
    }
}

class OffsetTransform(val offset: Instant = Clock.System.now())

internal const val MISSING_TICK_TRAFO_ERROR = "Tick transformation not configured."


// also provide transforms via direct environment extensions for use outside of simulation context. *?

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asTicks(duration: Duration): Double = duration.asTicks()
fun Environment.asDuration(duration: Number?): Duration? = duration?.let{
    tickTransform.ticks2Duration(duration.toDouble())
}

/** Transforms an wall `Instant` to simulation time.*/
fun Environment.toTickTime(instant: Instant) = instant.toTickTime()


/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallTime(time: TickTime) = time.asWallTime()
//fun Environment.asWallTimeOrNull(time: TickTime) = time.asWallTime()

//operator fun Instant.plus(duration: Duration): Instant = this.plus(duration.toJavaDuration())
