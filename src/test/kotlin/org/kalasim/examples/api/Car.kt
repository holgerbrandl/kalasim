import org.kalasim.*
import kotlin.time.Duration.Companion.minutes

fun main() {
    class Driver : Resource()
    class TrafficLight : State<String>("red")

    class Car : Component() {

        val trafficLight = get<TrafficLight>()
        val driver = get<Driver>()

        override fun process() = sequence {
            request(driver) {
                hold(1.minutes, description = "driving")

                wait(trafficLight, "green")
            }
        }
    }

    createSimulation {
        enableComponentLogger()

        dependency { TrafficLight() }
        dependency { Driver() }

        // finetune column width for website

        Car()
    }.run(5.0)
}
