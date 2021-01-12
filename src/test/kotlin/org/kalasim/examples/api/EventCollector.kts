//org.kalasim.examples.api.Cars.kts
import org.kalasim.*

class Driver : Resource()
class TrafficLight : State<String>("red")

class Car : Component() {

    val trafficLight = get<TrafficLight>()
    val driver = get<Driver>()

    override fun process() = sequence {
        request(driver) {
            hold(1.0)
        }

        wait(trafficLight, "green")
    }
}

createSimulation(enableConsoleLogger = true) {
    dependency { TrafficLight() }
    dependency { Driver() }

    Car()
}.run(5.0)