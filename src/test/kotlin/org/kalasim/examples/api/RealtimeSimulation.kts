import org.kalasim.ClockSync
import org.kalasim.createSimulation
import kotlin.time.Duration.Companion.seconds

val timeBefore = System.currentTimeMillis()

createSimulation(true) {
    // enable real-time clock synchronization
    ClockSync(tickDuration = 1.seconds)

    run(10)
}

println("time passed ${System.currentTimeMillis() - timeBefore})")