package org.kalasim

import org.kalasim.misc.TRACE_DF
import org.koin.core.component.KoinComponent
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
        val durationSinceOffset = when(tickUnit) {
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
    override fun durationAsTicks(duration: Duration): Double = when(tickUnit) {
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


@Suppress("EXPERIMENTAL_API_USAGE")
interface SimContext : KoinComponent {

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
fun Environment.asTickTime(instant: Instant)= instant.asTickTime()



/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallTime(time: TickTime)= time.asWallTime()