//Traffic.kts
import org.kalasim.*
import org.koin.core.component.get
import org.koin.core.component.inject

class TrafficLight : State<String>("red")

class GasStation(numPumps: Int = 6) : Resource(capacity = numPumps)

class Car(val trafficLight: TrafficLight) : Component() {

    val gasStation by inject<GasStation>()

    override suspend fun ProcContext.process() {
        // wait until the traffic light is green
        yield(wait(trafficLight, "green"))

        // request a slot in the gas-station
        yield(request(gasStation))

        // refill
        hold(5)

        // release pump
        release(gasStation)

        // change state of car to DATA
        yield(passivate())
    }
}


val env: Environment = configureEnvironment(true) {
    // Add a traffic light so that we can refer to it via koin get<T>()
    add { TrafficLight() }

    // Also add a resource with a limited capacity
    add { GasStation(2) }

    // we could add a car here...
    add { Car(get()) }

}.apply {
    // ... but since a car is not used as dependency elsewhere we
    //     can also create them in here
    val car2 = Car(get())
    val car3 = Car(get())

    // run for 10 ticks
    run(10)

    // toggle the traffic light
    get<TrafficLight>().value = "green"

    // run for another 10 ticks
    run(10)

    // assess the state of the simulation entities
    car2.statusMonitor.printHistogram()
    get<GasStation>().printStatistics()
}
