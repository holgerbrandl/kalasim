import org.kalasim.*

class MyEvent(val context: String, time: SimTime) : Event(time)

createSimulation {

    object : Component("Something") {
        override fun process() = sequence<Component> {
            //... a great history
            log(MyEvent("foo", now))
            //... a promising future
        }
    }

    // register to these events from the environment level
    addEventListener<MyEvent> { println(it.context) }

    run()
}
