package org.kalasim.letsplot

import org.kalasim.*
import kotlin.time.Duration.Companion.minutes
import org.kalasim.plot.letsplot.display

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

    val sim = createSimulation(enableComponentLogger = true) {
        dependency { TrafficLight() }
        dependency { Driver() }

        Car()
    }

    sim.run(5.0)

    val stateTimeline = sim.queue.first().stateTimeline

    stateTimeline.display().show()
}