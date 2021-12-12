import org.kalasim.*

class MyEvent(val context: String, time: TickTime) : Event(time)

createSimulation {
    addEventListener<MyEvent>{ println(it.context)}
}
