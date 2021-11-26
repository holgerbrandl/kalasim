package org.kalasim.scratch

import krangl.asDataFrame
import org.kalasim.*
import org.kalasim.plot.kravis.display
import org.kalasim.monitors.printConsole
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

        val cg = ComponentGenerator(exponential(1), keepHistory = true) { Vehicle() }

        run(1000)

        //gather arrival data
        val pStats: List<ComponentLifecycleRecord> = cg.history.map { it.toLifeCycleRecord() }

        // convert to krangl data-frame
        pStats.asDataFrame().printThis()


        cg.history.first().statusTimeline.summed().printConsole()
        cg.history.first().statusTimeline.summed().display()
    }
}
