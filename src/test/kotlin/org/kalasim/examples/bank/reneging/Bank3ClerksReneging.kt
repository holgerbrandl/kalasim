//Bank3ClerksReneging.kt
package org.kalasim.examples.bank.reneging

import org.kalasim.*
import org.kalasim.misc.printThis
import org.koin.core.component.inject


//**{todo}** use monitors here and maybe even inject them
//to inject use data class Counter(var value: Int)
var numBalked: Int = 0
var numReneged: Int = 0

class CustomerGenerator : Component() {

    override fun process() = sequence {
        while (true) {
            Customer(get())
            hold(uniform(5.0, 15.0).sample())
        }
    }
}

class Customer(val waitingLine: ComponentQueue<Customer>) : Component() {
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        if (waitingLine.size >= 5) {
            numBalked++

            log("balked")
            cancel()
        }

        waitingLine.add(this@Customer)

        for (c in clerks) {
            if (c.isPassive) {
                c.activate()
                break // activate only one clerk
            }
        }

        hold(50.0) // if not serviced within this time, renege

        if (waitingLine.contains(this@Customer)) {
            //  this@Customer.leave(waitingLine)
            waitingLine.remove(this@Customer)

            numReneged++
            log("reneged")
        } else {
            // if customer no longer in waiting line,
            // serving has started meanwhile
            passivate()
        }
    }
}


class Clerk : Component() {
    private val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while (true) {
            if (waitingLine.isEmpty())
                passivate()

            val customer = waitingLine.poll()
            customer.activate() // get the customer out of it's hold(50)

            hold(30.0) // bearbeitungszeit
            customer.activate() // signal the customer that's all's done
        }
    }
}


fun main() {
    val env = configureEnvironment(true) {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { (0..2).map { Clerk() } }
    }

    env.apply {
        // register other components to  be present when starting the simulation
        CustomerGenerator()

        val waitingLine: ComponentQueue<Customer> = get()

        waitingLine.lengthOfStayMonitor.disable()
        run(1500.0)

        waitingLine.lengthOfStayMonitor.enable()
        run(500.0)

        // with console
        waitingLine.lengthOfStayMonitor.printHistogram()
        waitingLine.queueLengthMonitor.printHistogram()

        // with kravis
//        waitingLine.queueLengthMonitor.display()
//        waitingLine.lengthOfStayMonitor.display()

        waitingLine.stats.toJson().toString(2).printThis()

        println("number reneged: $numReneged")
        println("number balked: $numBalked")
    }
}
