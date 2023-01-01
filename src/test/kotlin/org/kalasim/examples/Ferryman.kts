//Ferryman.kts
package org.kalasim.examples

import org.kalasim.*
import org.kalasim.monitors.NumericStatisticMonitor
import org.kalasim.plot.kravis.display
import kotlin.time.Duration.Companion.minutes

createSimulation {

    class Passenger : Component()

    val fm = object : Component("ferryman") {
        val left2Right = ComponentQueue<Passenger>()
        val right2Left = ComponentQueue<Passenger>()

        val l2rMonitor = NumericStatisticMonitor()
        val r2lMonitor = NumericStatisticMonitor()

        override fun process() = sequence {
            val batchLR: List<Passenger> = batch(left2Right, 4, timeout = 10.minutes)
            l2rMonitor.addValue(batchLR.size)
            hold(5.minutes, description = "shipping ${batchLR.size} l2r")

            val batchRL: List<Passenger> = batch(right2Left, 4, timeout = 10.minutes)
            r2lMonitor.addValue(batchRL.size)
            hold(5.minutes, description = "shipping ${batchRL.size} r2l")

            // we could also use an infinite while loop instead of activate
            activate(process = Component::process)
        }
    }

    ComponentGenerator(uniform(0, 15)) { Passenger() }
        .addConsumer { fm.left2Right.add(it) }

    ComponentGenerator(uniform(0, 12)) { Passenger() }
        .addConsumer { fm.right2Left.add(it) }

    run(10000.minutes)

    fm.l2rMonitor.display("Passengers left->right")
    fm.r2lMonitor.display("Passengers right->left")
}
