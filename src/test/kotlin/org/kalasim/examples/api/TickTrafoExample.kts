//TickTrafoExample.kts
import org.kalasim.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit


createSimulation(true) {
    tickTransform = OffsetTransform(Instant.now(), TimeUnit.MINUTES)

    // run until a specific time
    run(until = asTickTime(Instant.now().plus(Duration.ofHours(1))))

    // run for a given duration
    run(asTickDuration(Duration.ofHours(1)))

    println(asWallTime(now))
}