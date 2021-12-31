import org.kalasim.*

class MyEvent(val context: String, time: TickTime) : Event(time)

createSimulation {

    object : Component("Something") {
        override fun process() = sequence<Component> {
            //... a great history
            log(MyEvent("foo", now))
            //... a promising future
        }
    }

    addEventListener<MyEvent> { println(it.context) }

    run()
}
