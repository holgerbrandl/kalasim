//EventCollector.kts
import org.kalasim.*

createSimulation(enableConsoleLogger = true) {
    val tc = eventLog()

    tc.filter { it is InteractionEvent && it.source?.name == "foo" }

    val claims = tc //
        .filterIsInstance<ResourceEvent>()
        .filter { it.type == ResourceEventType.CLAIMED }
}.run(5.0)