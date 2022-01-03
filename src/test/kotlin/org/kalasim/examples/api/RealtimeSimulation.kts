import org.kalasim.ClockSync
import org.kalasim.createSimulation
import kotlin.time.Duration.Companion.seconds

val timeBefore = System.currentTimeMillis()

createSimulation(true) {
    // enable real-time clock synchronization w
    ClockSync(tickDuration = 1.seconds)

    // enable real-time clock synchronization but run in 2x realtime
//    ClockSync(Duration.ofSeconds(1), speedUp = 2)

    run(10)
}

println("time passed ${System.currentTimeMillis() - timeBefore})")