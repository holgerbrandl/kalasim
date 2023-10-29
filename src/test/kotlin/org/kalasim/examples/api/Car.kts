//Cars.kts
import org.kalasim.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class Driver : Resource()
class TrafficLight : State<String>("red")

class Car : Component() {

    val trafficLight = get<TrafficLight>()
    val driver = get<Driver>()

    override fun process() = sequence {
        request(driver) {
            hold(30.minutes, description = "driving")

            wait(trafficLight, "green")
        }
    }
}

createSimulation {
    enableComponentLogger()

    dependency { TrafficLight() }
    dependency { Driver() }

    Car()
}.run(5.hours)

