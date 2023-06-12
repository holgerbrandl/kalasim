package org.kalasim.scratch.shipyard

import org.kalasim.*
import org.kalasim.plot.kravis.display

// Wait for a terminal to accumulate enough goods before starting shipment.
//
// In contrast to ShipyardState.kt, we here do ot rely on a state for interation, but use built-in request capabilities and a depletable resource to wait until enough cargo has accumulated.
fun main() {
    createSimulation {
        enableComponentLogger()

        val terminal = object : Component("terminal") {
            val tank = DepletableResource(capacity = 100, initialLevel = 10)

            override fun process() = sequence {
                while(true) {
                    hold(1)
                    put(tank, 2)
                    log("tankLevel is ${tank.available}")
                }
            }
        }

        object : Component("ship") {
            override fun process() = sequence {
                request(terminal.tank withQuantity 30, description = "waiting for goods") {
                    hold(30, "shipping goods")
                }
            }
        }

        run(50)

        terminal.tank.availabilityTimeline.display()
    }
}



