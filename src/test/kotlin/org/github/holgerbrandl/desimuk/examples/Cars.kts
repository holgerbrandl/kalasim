import org.github.holgerbrandl.desimuk.Component
import org.github.holgerbrandl.desimuk.Environment
import org.github.holgerbrandl.desimuk.examples.koiner.createSimulation

class Car : Component() {
    override suspend fun SequenceScope<Component>.process() {
            // wait for 1 sec
            yield(hold(1.0))
            // and terminate it
            yield(terminate())
    }
}

createSimulation{
    Car()
}.run(5.0)
