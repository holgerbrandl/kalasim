import org.kalasim.*

data class Counter(var value: Int)

class Something(val counter: Counter): Component(){

    override suspend fun ProcContext.process() {
        counter.value++
    }
}
configureEnvironment {
    add { Counter(0)}
    add { Something(get())}
}.run(10)