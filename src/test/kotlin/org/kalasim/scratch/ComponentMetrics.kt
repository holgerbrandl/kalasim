package org.kalasim.scratch

import krangl.asDataFrame
import org.kalasim.*
import org.kalasim.misc.display
import org.kalasim.misc.printConsole
import org.kalasim.misc.printThis

fun main() {
    createSimulation(true) {
        val fuelPump = Resource("tank", capacity = 3)
        val refilPermitted = State(false)

        object : Component("refillController") {
            override fun process() = sequence<Component> {
                while(true) {
                    hold(uniform(0, 10)())
                    refilPermitted.value = true
                    hold(uniform(0, 10)())
                    refilPermitted.value = false
                }
            }
        }

        class Vehicle : Component() {
            val uni = uniform(0, 10)

            override fun process() = sequence {
                hold(uni())
                wait(refilPermitted, true)
                request(fuelPump) {
                    hold(3)
                }
            }
        }

        val cg = ComponentGenerator(exponential(1), storeRefs = true) { Vehicle() }

        run(1000)

        //gather arrival data
        val pStats: List<ComponentLifecycleRecord> = cg.arrivals.map { it.toLifeCycleRecord() }

        // konvert to krangl data-frame
        pStats.asDataFrame().printThis()


        cg.arrivals.first().statusMonitor.summed().printConsole()
        cg.arrivals.first().statusMonitor.summed().display()
    }
}
