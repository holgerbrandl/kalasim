//TickTrafoExample.kts
import org.kalasim.OffsetTransform
import org.kalasim.asWallTime
import org.kalasim.createSimulation
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit


createSimulation(true) {
    tickTransform = OffsetTransform(Instant.now(), TimeUnit.MINUTES)

    // run until a specific time
    run(until = Instant.now().plus(Duration.ofHours(1)).asTickTime())

    // run for a given duration
    run(Duration.ofHours(1).asTicks())

    println(asWallTime(now))
}