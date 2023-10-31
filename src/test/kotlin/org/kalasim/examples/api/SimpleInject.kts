//SimpleInject.kts
import org.kalasim.*

data class Counter(var value: Int)

class Something(val counter: Counter) : Component() {

    override fun process() = sequence<Component> {
        counter.value++
    }
}
createSimulation {
    dependency { Counter(0) }
    dependency { Something(get()) }

    run(10)
}