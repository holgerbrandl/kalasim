//EventCollector.kts
import org.kalasim.*
import org.kalasim.analysis.*

createSimulation {
    enableComponentLogger()

    // enable a global list that will capture all events excluding StateChangedEvent
    val eventLog = enableEventLog(blackList = listOf(StateChangedEvent::class))

    // run the simulation
    run(5.seconds)

    eventLog.filter { it is InteractionEvent && it.component?.name == "foo" }

    val claims = eventLog //
        .filterIsInstance<ResourceEvent>()
        .filter { it.type == ResourceEventType.CLAIMED }
}