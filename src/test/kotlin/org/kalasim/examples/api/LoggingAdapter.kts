//LoggingAdapter.kts
import org.kalasim.examples.er.EmergencyRoom
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

// Create simulation
val er = EmergencyRoom()

// Add a custom event handler to forward events to the used logging library
er.addEventListener { event ->
    // resolve the event type to a dedicated logger to allow fine-grained control
    val logger = Logger.getLogger(event::class.java.name)

    logger.info { event.toString() }
}

// Run the sim
er.run(100.days)