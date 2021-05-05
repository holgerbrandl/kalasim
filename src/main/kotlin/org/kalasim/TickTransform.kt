package org.kalasim

import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit




@Suppress("EXPERIMENTAL_FEATURE_WARNING")
 inline class TickTime(val instant: Number)

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
    fun tick2wallTime(tickTime: Double): Instant
    fun wall2TickTime(instant: Instant): Double
    fun durationAsTicks(duration: Duration): Double
}

class OffsetTransform(val offset: Instant = Instant.now(), val tickUnit: TimeUnit = TimeUnit.MINUTES) : TickTransform {
    override fun tick2wallTime(tickTime: Double): Instant {
        val durationSinceOffset = when(tickUnit){
            TimeUnit.NANOSECONDS -> Duration.ofNanos(tickTime.toLong())
            TimeUnit.MICROSECONDS -> Duration.ofNanos((tickTime * 1000).toLong())
            TimeUnit.MILLISECONDS -> Duration.ofNanos((tickTime * 1000000).toLong())
            TimeUnit.SECONDS -> Duration.ofMillis((tickTime * 1000).toLong())
            TimeUnit.MINUTES -> Duration.ofMillis((tickTime * 60000).toLong())
            TimeUnit.HOURS -> Duration.ofSeconds((tickTime * 3600).toLong())
            TimeUnit.DAYS -> Duration.ofMinutes((tickTime * 1440).toLong())
        }

        return offset + durationSinceOffset
    }

    override fun wall2TickTime(instant: Instant): Double {
        val offsetDuration = Duration.between(offset, instant)

        return durationAsTicks(offsetDuration)
    }

    // todo improve precision of transformation
    override fun durationAsTicks(duration: Duration): Double = when(tickUnit){
        TimeUnit.NANOSECONDS -> duration.toNanos()
        TimeUnit.MICROSECONDS -> duration.toNanos()/1000.0
        TimeUnit.MILLISECONDS -> duration.toNanos()/1000000.0
        // https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.SECONDS -> duration.toMillis()/1000.0
        TimeUnit.MINUTES -> duration.toMillis()/60000.0
        //https://stackoverflow.com/questions/42317152/why-does-the-duration-class-not-have-toseconds-method
        TimeUnit.HOURS -> duration.seconds /3600.0
        TimeUnit.DAYS -> duration.toMinutes()/1440.0
    }.toDouble()
}

internal val MISSING_TICK_TRAFO_ERROR = "Tick transformation not configured. "

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallTime(tickTime: Double): Instant {
    require(tickTransform != null){ MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.tick2wallTime(tickTime)
}

/** Transforms a simulation time (typically `now`) to the corresponding wall time. */
fun Environment.asWallDuration(tickTime: Double): Instant {
    require(tickTransform != null){ MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.tick2wallTime(tickTime)
}

/** Transforms a wall `duration` into the corresponding amount of ticks.*/
fun Environment.asTicks(duration: Duration): Double {
    require(tickTransform != null){ MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.durationAsTicks(duration)
}

/** Transforms a wall `duration` into the corresponding amount of ticks.*/
fun Component.asTicks(duration: Duration) = env.asTicks(duration)

// note: There is also an extension on Duration in Environment (because of missing multiple receiver support)

/** Transforms an wall `Instant` to simulation time.*/
fun Environment.asTickTime(instant: Instant): Double {
    require(tickTransform != null){ MISSING_TICK_TRAFO_ERROR }
    return tickTransform!!.wall2TickTime(instant)
}

