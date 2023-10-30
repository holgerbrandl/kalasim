import org.kalasim.*
import org.kalasim.misc.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class Driver : Resource(trackingConfig = ResourceTrackingConfig(trackUtilization = false))
class TrafficLight : State<String>("red", trackingConfig = StateTrackingConfig(logCreation = false))

class Car : Component(
    trackingConfig = ComponentTrackingConfig(logInteractionEvents = false)
) {

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

    // in addition or alternatively we can also change the environment defaults
    entityTrackingDefaults.DefaultComponentConfig =
        ComponentTrackingConfig(logStateChangeEvents = false)

    // create simulation entities
    dependency { TrafficLight() }
    dependency { Driver() }

    Car()
}.run(5.hours)