import org.github.holgerbrandl.kalasim.Component
import org.github.holgerbrandl.kalasim.configureEnvironment
import org.github.holgerbrandl.kalasim.createSimulation

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
