//EventCollector.kts
import org.kalasim.*

createSimulation(enableConsoleLogger = true) {
    val tc = traceCollector()

    tc.filter { it.source?.name == "foo" }

    val claims = tc //
        .filterIsInstance<ResourceEvent>()
        .filter{ it.type == ResourceEventType.CLAIMED}
}.run(5.0)