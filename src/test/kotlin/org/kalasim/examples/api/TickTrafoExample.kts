//TickTrafoExample.kts
import org.kalasim.*
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration


createSimulation(true) {
//    tickTransform = OffsetTransform(Instant.now(), TimeUnit.MINUTES)
    tickTransform = TickTransform(TimeUnit.MINUTES)

    object :Component(){
        override fun process() =sequence {
            hold(1.minutes, description = "dressing")

            // we can express fractional durations (1.3 hours = 78 minutes)
            hold(1.3.hours, description = "walking")

            // we can also hold until a specific time
            hold(until= now + 3.hours )
        }
    }

    // run until a specific time
    run(until = Instant.now() + 1.hours)

    // run for a given duration
    run(1.days)

    println(now)
    println(asWallTime(now))
}

operator fun Instant.plus(duration: kotlin.time.Duration) = this + duration.toJavaDuration()
