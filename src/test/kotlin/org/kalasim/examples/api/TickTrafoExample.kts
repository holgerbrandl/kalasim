//TickTrafoExample.kts
import kotlinx.datetime.Clock
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit


// note MINUTES is also kalasim's default
createSimulation(durationUnit = DurationUnit.MINUTES) {
    enableComponentLogger()

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
    run(until = Clock.System.now() + 1.hours)

    // run for a given duration
    run(1.days)

    println(now)
    println(toWallTime(now))
}
