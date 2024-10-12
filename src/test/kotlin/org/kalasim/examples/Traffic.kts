//Traffic.kts
import org.kalasim.*
import org.koin.core.component.inject

enum class TrafficLightState { RED, GREEN }

/** A traffic light with 2 states. */
class TrafficLight : State<TrafficLightState>(TrafficLightState.RED) {

    fun toggleState() {
        when(value) {
            TrafficLightState.RED -> TrafficLightState.GREEN
            TrafficLightState.GREEN -> TrafficLightState.RED
        }
    }
}


/** A simple controller that will toggle the state of the traffic-light */
class TrafficLightController(val trafficLight: TrafficLight) : Component() {

    override fun repeatedProcess() = sequence {
        hold(6)
        trafficLight.toggleState()
    }
}

/** A gas station, where cars will stop for refill. */
class GasStation(numPumps: Int = 6) : Resource(capacity = numPumps)

/** A car with a process definition detailing out its way to the gas-station via a crossing. */
class Car(val trafficLight: TrafficLight) : Component() {

    val gasStation by inject<GasStation>()

    override fun process() = sequence {
        // Wait until the traffic light is green
        wait(trafficLight, TrafficLightState.GREEN)

        // Request a slot in the gas-station
        request(gasStation) {
            hold(5, description = "refilling")
        }
    }
}

createSimulation {
    enableComponentLogger()

    // Add a traffic light so that we can refer to it via koin get<T>()
    dependency { TrafficLight() }

    // Also add a resource with a limited capacity
    dependency { GasStation(2) }

    // Setup a traffic light controller to toggle the light
    TrafficLightController(get())

    // Setup a car generator with an exponentially distributed arrival time
    ComponentGenerator(exponential(7).minutes) { Car(get()) }

    // enable component tracking for later analytics
    val cg = componentCollector()

    // Run for 30 ticks
    run(10)

    // Toggle the traffic light manually
    get<TrafficLight>().value = TrafficLightState.GREEN

    // Run for another 10 ticks
    run(10)

    // Assess the state of the simulation entities
    cg.filterIsInstance<Car>().first().stateTimeline.printHistogram()
    get<GasStation>().printStatistics()
}
