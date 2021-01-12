import org.kalasim.ClockSync
import org.kalasim.createSimulation
import java.time.Duration

val timeBefore = System.currentTimeMillis()

createSimulation(true) {
    // TODO Define components, resources, states and interactions

    // enable real-time clock synchronization w
    ClockSync(tickDuration = Duration.ofSeconds(1))

    // enable real-time clock synchronization but run in 2x realtime
//    ClockSync(Duration.ofSeconds(1), speedUp = 2)

    run(10)
}

println("time passed ${System.currentTimeMillis() - timeBefore})")