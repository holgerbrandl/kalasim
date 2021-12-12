package org.kalasim.scratch.shipyard

import org.kalasim.*
import org.kalasim.plot.kravis.display

// wait for a terminal to accumulate enough goods before starting shipment. In contrast to ResourcfulComponent.kt, we here do ot rely on a state for interation, but use built-in request capabilities to wait until enough material has accumulated.
fun main() {
    createSimulation(true) {

        val terminal = object : Component("terminal") {
            val tank = DepletableResource(capacity = 100, initialLevel = 10)

            override fun process() = sequence {
                while (true) {
                    hold(1)
                    put(tank withQuantity 2)
                    log("tankLevel is ${tank.availableQuantity}")
                }
            }
        }

        object : Component("ship") {
            override fun process() = sequence {
                request(terminal.tank withQuantity 30, description= "waiting for goods") {
                    hold(30, "shipping goods")
                }
            }
        }

        run(50)

        terminal.tank.availabilityTimeline.display()
    }
}



