import org.github.holgerbrandl.desimuk.Component
import org.github.holgerbrandl.desimuk.Environment

class Car : Component() {
    override suspend fun SequenceScope<Component>.process() {
        while (true) {
            // wait for 1 sec
            yield(hold(1.0))
            // and terminate it
            yield(terminate())
        }
    }
}

Environment().apply {
    Car()
}.run(5.0)
