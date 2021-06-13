package org.kalasim

import org.kalasim.misc.TRACE_DF
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit


@JvmInline
value class TickTime(val value: Double) : Comparable<TickTime> {
    override operator fun compareTo(other: TickTime): Int = value.compareTo(other.value)
    operator fun minus(duration: Double): TickTime = TickTime(value - duration)
    operator fun plus(duration: Number): TickTime = TickTime(value + duration.toDouble())

    operator fun minus(other: TickTime): Double = value - other.value


    constructor(instant: Number) : this(instant.toDouble())

    override fun toString(): String {
        return TRACE_DF.format(value)
    }
}

/* A simple type wrapper around a duration in sim time coordinates. Not used in core API of kalasim. */
@JvmInline
value class Ticks(val value: Double) {
    constructor(instant: Number) : this(instant.toDouble())
}


//val Number.ticks: Ticks
//    get() = Ticks(this)

val Number.tickTime: TickTime
    get() = TickTime(this)

val Number.tt: TickTime
    get() = TickTime(this)

fun Number.asTickTime() = TickTime(this)


//infix fun Number.tt(): TickTime = TickTime(this)
//
//fun main() {
//    val foo = 1.tickTime
//}
//
////    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
//data class TickTime(val env: Environment, val time: Number){
//    operator fun plus(duration: Number): TickTime = TickTime(env,this.time.toDouble() + duration.toDouble())
//    operator fun plus(duration: Duration, env:Environment) =  TickTime(env,this.time.toDouble() + env.asTicks(duration))
//}
//
//
//private val Number.tickTime
//    get() = TickTime(this)


// https://stackoverflow.com/questions/32437550/whats-the-difference-between-instant-and-localdatetime
interface TickTransform {
    fun tick2wallTime(tickTime: TickTime): Instant
    fun wall2TickTime(instant: Instant): TickTime
    fun durationAsTicks(duration: Duration): Double
}

class OffsetTransform(val offset: Instant = Instant.now(), val tickUnit: TimeUnit = TimeUnit.MINUTES) : TickTransform {
    override fun tick2wallTime(tickTime: TickTime): Instant {
        val ttValue = tickTime.value
        val durationSinceOffset = when (tickUnit) {
            TimeUnit.NANOSECONDS -> Duration.ofNanos(ttValue.toLong())
            TimeUnit.MICROSECONDS -> Duration.ofNanos((ttValue * 1000).toLong())
            TimeUnit.MILLISECONDS -> Duration.ofNanos((ttValue * 1000000).toLong())
            TimeUnit.SECONDS -> Duration.ofMillis((ttValue * 1000).toLong())
            TimeUnit.MINUTES -> Duration.ofMillis((ttValue * 60000).toLong())
            TimeUnit.HOURS -> Duration.ofSeconds((ttValue * 3600).toLong())
            TimeUnit.DAYS -> Duration.ofMinutes((ttValue * 1440).toLong())
        }

        return offset + durationSinceOffset
    }

    override fun wall2TickTime(instant: Instant): TickTime {
        val offsetDuration = Duration.between(offset, instant)

        return TickTime(durationAsTicks(offsetDuration))
    }

    // todo improve precision of transformation
    override fun durationAsTicks(duration: Duration): Double = when (tickUnit) {
        TimeUnit.NANOSECONDS -> duration.toNanos()
        TimeUnit.MICROSECONDS -> duration.toNanos() / 1000.0
        TimeUnit.MILLISECONDS -> duration.toNanos() / 1000000.0
        // https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.SECONDS -> duration.toMillis() / 1000.0
        TimeUnit.MINUTES -> duration.toMillis() / 60000.0
        //https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.HOURS -> duration.seconds / 3600.0
        TimeUnit.DAYS -> duration.toMinutes() / 1440.0
    }.toDouble()
}

internal val MISSING_TICK_TRAFO_ERROR = "Tick transformation not configured. "

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
@Deprecated("Passe TickTime instead of raw double time")
fun Environment.asWallTime(tickTime: Double): Instant {
    require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.tick2wallTime(tickTime.tickTime)
}

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallTime(tickTime: TickTime): Instant {
    require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.tick2wallTime(tickTime)
}

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
//fun Environment.asWallDuration(tickTime: Double): Instant {
//    require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
//    return tickTransform!!.tick2wallTime(tickTime)
//}

/** Transforms a wall `duration` into the corresponding amount of ticks.*/
fun Environment.asTicks(duration: Duration): Double {
    require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.durationAsTicks(duration)
}

/** Transforms a wall `duration` into the corresponding amount of ticks.*/
fun Component.asTicks(duration: Duration) = env.asTicks(duration)

// note: There is also an extension on Duration in Environment (because of missing multiple receiver support)

/** Transforms an wall `Instant` to simulation time.*/
fun Environment.asTickTime(instant: Instant): TickTime {
    require(tickTransform != null) { MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.wall2TickTime(instant)
}

