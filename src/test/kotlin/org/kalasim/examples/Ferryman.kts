//Ferryman.kts
package org.kalasim.examples

import org.kalasim.*
import org.kalasim.misc.display

createSimulation {

    // process set to null here to prevent them from polluting the event queue
    class Passenger : Component(process = null)

    val fm = object : Component("ferryman") {
        val left2Right = ComponentQueue<Passenger>("queue(left->right)")
        val right2Left = ComponentQueue<Passenger>("queue(right->left)")

        val l2rMonitor = NumericStatisticMonitor()
        val r2lMonitor = NumericStatisticMonitor()

        override fun process() = sequence {
            val batchLR: List<Component> = batch(left2Right, 4, timeout = 10)
            l2rMonitor.addValue(batchLR.size)
            hold(5, description = "shipping ${batchLR.size} l2r")

            val batchRL: List<Component> = batch(right2Left, 4, timeout = 10)
            r2lMonitor.addValue(batchRL.size)
            hold(5, description = "shipping ${batchRL.size} r2l")

            // we could also use an infinite while loop instead of activate
            yield(activate(process = Component::process))
        }
    }

    ComponentGenerator(uniform(0, 15)) { Passenger() }
        .addConsumer { fm.left2Right.add(it) }

    ComponentGenerator(uniform(0,12)) { Passenger() }
        .addConsumer { fm.right2Left.add(it) }

    run(10000)

    fm.l2rMonitor.display("Queue left->right")
    fm.r2lMonitor.display("Queue right->left")
}
