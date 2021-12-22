//EventCollector.kts
import org.kalasim.*
import org.kalasim.analysis.InteractionEvent
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType

createSimulation(enableConsoleLogger = true) {
    val tc = eventLog()

    tc.filter { it is InteractionEvent && it.source?.name == "foo" }

    val claims = tc //
        .filterIsInstance<ResourceEvent>()
        .filter { it.type == ResourceEventType.CLAIMED }
}.run(5.0)