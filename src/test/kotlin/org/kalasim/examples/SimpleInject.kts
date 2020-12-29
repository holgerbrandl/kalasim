//SimpleInject.kts
import org.kalasim.Component
import org.kalasim.ProcContext
import org.kalasim.add
import org.kalasim.configureEnvironment

data class Counter(var value: Int)

class Something(val counter: Counter) : Component() {

    override suspend fun ProcContext.process() {
        counter.value++
    }
}
configureEnvironment {
    add { Counter(0) }
    add { Something(get()) }
}.run(10)