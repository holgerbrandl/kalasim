//LoggingAdapter.kts
import org.kalasim.analysis.ResourceEvent
import org.kalasim.examples.er.EmergencyRoom
import java.util.logging.Logger


// Create simulation
val er = EmergencyRoom()

val LOG: Logger = Logger.getLogger(this::class.java.getName())

// Add a custom event handler to forward events to the used logging library
er.addEventListener<ResourceEvent> {
    LOG.info(it.action?: "")
}

// Run the sim for 100 ticks
er.run(100.0)