package org.kalasim.scratch.shipyard

import org.kalasim.*
import org.kalasim.plot.kravis.display
import kotlin.time.Duration.Companion.minutes

// Wait for a terminal to accumulate enough goods before starting shipment.
//
// This implementation essentially does not request until the resource level is sufficient for the request. This is just kept for educational reasons, although it is not wrong as detailed out below in the comment
fun main() {
    createSimulation {

        val terminal = object : Component() {
            val tank = DepletableResource(capacity = 10)
            val tankState = State(false)

            override fun process() = sequence {
                while (true) {
                    hold(1.minutes)

                    put(tank, 2)
                    if (tank.level > 10) {
                        tankState.value = true
                    }
                }
            }
        }

        object : Component() {
            override fun process() = sequence {
                // FIXME: Actually, using wait(terminal.tankState) is a false friend here, as multiple ships would be triggered if the terminal has sufficient fuel, even if conceptually not all requests could be honored. So the correct solution is to use ShipyardRequest.kt

                wait(terminal.tankState, true)
                hold(30.minutes, "shipping goods")
            }
        }

        run(50)

        terminal.tank.availabilityTimeline.display()
    }
}
