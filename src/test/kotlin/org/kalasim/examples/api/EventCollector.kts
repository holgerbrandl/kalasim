//EventCollector.kts
import org.kalasim.analysis.*
import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger
import org.kalasim.enableEventLog

createSimulation {
    enableComponentLogger()

    val tc = enableEventLog()

    tc.filter { it is InteractionEvent && it.component?.name == "foo" }

    val claims = tc //
        .filterIsInstance<ResourceEvent>()
        .filter { it.type == ResourceEventType.CLAIMED }
}.run(5.0)