//Traffic.kts
import org.kalasim.*
import org.koin.core.component.inject

class TrafficLight : State<String>("red")

class GasStation(numPumps: Int = 6) : Resource(capacity = numPumps)

class Car(val trafficLight: TrafficLight) : Component() {

    val gasStation by inject<GasStation>()

    override fun process() = sequence {
        // wait until the traffic light is green
        wait(trafficLight, "green")

        // request a slot in the gas-station
        request(gasStation)

        // refill
        hold(5)

        // release pump
        release(gasStation)

        // change state of car to DATA
        passivate()
    }
}

class Car2 : Component() {

    val gasStation by inject<GasStation>()

    override fun process() = sequence {
        // wait until the traffic light is green
        request(gasStation) {
            hold(2, "refill")
        }

        val trafficLight = get<TrafficLight>()
        wait(trafficLight, "green")
    }
}

        createSimulation(true) {
            // Add a traffic light so that we can refer to it via koin get<T>()
            dependency { TrafficLight() }

            // Also add a resource with a limited capacity
            dependency { GasStation(2) }

            val car1 = Car(get())
            val car2 = Car(get())
            val car3 = Car(get())

            // run for 10 ticks
            run(10)

            // toggle the traffic light
            get<TrafficLight>().value = "green"

            // run for another 10 ticks
            run(10)

            // assess the state of the simulation entities
            car2.statusTimeline.printHistogram()
            get<GasStation>().printStatistics()
        }
