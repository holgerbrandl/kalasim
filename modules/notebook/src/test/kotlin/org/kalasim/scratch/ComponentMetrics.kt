package org.kalasim.scratch

import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.kalasim.*
import org.kalasim.monitors.printConsole
import org.kalasim.plot.kravis.display
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

fun main() {
    createSimulation {
        enableComponentLogger()

        val fuelPump = Resource("tank", capacity = 3)
        val refilPermitted = State(false)

        object : Component("refillController") {
            override fun process() =
                sequence {
                    while(true) {
                        val uniform = uniform(0, 10).seconds

                        hold(uniform())
                        refilPermitted.value = true
                        hold(uniform())
                        refilPermitted.value = false
                    }
                }
        }

        class Vehicle : Component() {
            val uni = uniform(0, 10).seconds

            override fun process() =
                sequence {
                    hold(uni())
                    wait(refilPermitted, true)

                    request(fuelPump) {
                        hold(3.minutes)
                    }
                }
        }

        val cg = ComponentGenerator(exponential(1).seconds, keepHistory = true) { Vehicle() }

        run(5.hours)

        //gather arrival data
        val pStats: List<ComponentLifecycleRecord> = cg.history.map { it.toLifeCycleRecord() }

        // convert to krangl data-frame
        pStats.toDataFrame().print()


//        cg.history.first().stateTimeline.summed().printConsole()
//        cg.history.first().stateTimeline.summed().display()
    }
}
