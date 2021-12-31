//EventCollector.kts
import org.kalasim.analysis.*
import org.kalasim.createSimulation
import org.kalasim.eventLog

createSimulation(enableConsoleLogger = true) {
    val tc = eventLog()

    tc.filter { it is InteractionEvent && it.source?.name == "foo" }

    val claims = tc //
        .filterIsInstance<ResourceEvent>()
        .filter { it.type == ResourceEventType.CLAIMED }
}.run(5.0)