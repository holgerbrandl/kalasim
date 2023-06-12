package org.kalasim.scratch

import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.kalasim.*
import org.kalasim.monitors.printConsole
import org.kalasim.plot.kravis.display

fun main() {
    createSimulation {
        enableComponentLogger()

        val fuelPump = Resource("tank", capacity = 3)
        val refilPermitted = State(false)

        object : Component("refillController") {
            override fun process() =
                sequence {
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

            override fun process() =
                sequence {
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
        pStats.toDataFrame().print()


        cg.history.first().stateTimeline.summed().printConsole()
        cg.history.first().stateTimeline.summed().display()
    }
}
