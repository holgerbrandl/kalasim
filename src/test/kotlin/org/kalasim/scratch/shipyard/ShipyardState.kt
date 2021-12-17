package org.kalasim.scratch.shipyard

import org.kalasim.Component
import org.kalasim.DepletableResource
import org.kalasim.State
import org.kalasim.createSimulation
import org.kalasim.plot.kravis.display

// wait for a terminal to accumulate enough goods before starting shipment. This implementation essentially does not request until the resource level is sufficient for the request
fun main() {
    createSimulation {

        val terminal = object : Component() {
            val tankLevel = DepletableResource(capacity = 10)
            val tankState = State(false)

            override fun process() = sequence {
                while (true) {
                    hold(1)
                    tankLevel.capacity += 2
                    if (tankLevel.capacity > 10) {
                        tankState.value = true
                    }
                }
            }
        }

        val ship = object : Component() {
            override fun process() = sequence {
                wait(terminal.tankState, true)
                hold(30, "shipping goods")
            }
        }

        run(50)

        terminal.tankLevel.availabilityTimeline.display()
    }
}
