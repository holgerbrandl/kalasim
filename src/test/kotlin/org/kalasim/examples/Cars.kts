import org.kalasim.Component
import org.kalasim.configureEnvironment
import org.kalasim.createSimulation

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
