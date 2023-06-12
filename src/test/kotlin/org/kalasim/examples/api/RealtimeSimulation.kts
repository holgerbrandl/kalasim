import org.kalasim.ClockSync
import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger
import kotlin.time.Duration.Companion.seconds

val timeBefore = System.currentTimeMillis()

createSimulation {
    enableComponentLogger()

    // enable real-time clock synchronization
    ClockSync(tickDuration = 1.seconds)

    run(10)
}

println("time passed ${System.currentTimeMillis() - timeBefore})")