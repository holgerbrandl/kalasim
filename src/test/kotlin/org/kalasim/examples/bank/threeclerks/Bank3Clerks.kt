//Bank3Clerks.kt
package org.kalasim.examples.bank.threeclerks

import org.kalasim.*
import org.kalasim.plot.kravis.canDisplay
import org.kalasim.plot.kravis.display
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes


class CustomerGenerator : Component() {

    override fun process() = sequence {
        while(true) {
            Customer(get())
            hold(uniform(5.0, 15.0).minutes.sample())
        }
    }
}

class Customer(val waitingLine: ComponentQueue<Customer>) : Component() {
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        waitingLine.add(this@Customer)

        for(c in clerks) {
            if(c.isPassive) {
                c.activate()
                break // activate at max one clerk
            }
        }

        passivate()
    }
}


class Clerk : Component() {
    private val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while(true) {
            if(waitingLine.isEmpty())
                passivate()

            val customer = waitingLine.poll()
            hold(30.minutes) // bearbeitungszeit
            customer.activate() // signal the customer that's all's done
        }
    }
}


fun main() {
    createSimulation {
        dependency { ComponentQueue<Customer>("waitingline") }
        dependency { State(false, "worktodo") }
        dependency { CustomerGenerator() }
        dependency { (1..3).map { Clerk() } }

        run(50000.0)

        val waitingLine: ComponentQueue<Customer> = get()

        if(canDisplay()) {
//        waitingLine.lengthOfStayMonitor.printHistogram()
//        waitingLine.queueLengthMonitor.printHistogram()

            waitingLine.queueLengthTimeline.display()
            waitingLine.lengthOfStayStatistics.display()
        }

//        waitingLine.stats.toJson().toString(2).printThis()
        waitingLine.printSummary()
    }
}
