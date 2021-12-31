//SimpleInject.kts
import org.kalasim.*

data class Counter(var value: Int)

class Something(val counter: Counter) : Component() {

    override fun process() = sequence<Component> {
        counter.value++
    }
}
configureEnvironment {
    add { Counter(0) }
    add { Something(get()) }
}.run(10)