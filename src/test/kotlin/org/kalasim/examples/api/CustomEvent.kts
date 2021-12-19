import org.kalasim.Event
import org.kalasim.TickTime
import org.kalasim.createSimulation

class MyEvent(val context: String, time: TickTime) : Event(time)

createSimulation {
    addEventListener<MyEvent> { println(it.context) }
}
