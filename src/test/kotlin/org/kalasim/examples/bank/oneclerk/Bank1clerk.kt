//Bank1clerk.kt
package org.kalasim.examples.bank.oneclerk

import org.kalasim.*
import org.kalasim.plot.kravis.canDisplay
import org.kalasim.plot.kravis.display
import org.koin.core.component.inject


class Customer(
    val waitingLine: ComponentQueue<Customer>,
    val clerk: Clerk
) : Component() {
    override fun process() = sequence {
        waitingLine.add(this@Customer)

        if(clerk.isPassive) clerk.activate()

        passivate()
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while(true) {
            while(waitingLine.isEmpty()) passivate()

            val customer = waitingLine.poll()

            hold(10.0) // bearbeitungszeit
            customer.activate()
        }
    }
}

class CustomerGenerator : Component() {

    //    var numCreated  = 0
    override fun process() = sequence {
//        if(numCreated++ >5 ) return@sequence
        while(true) {
            Customer(get(), get())

            hold(uniform(5.0, 15.1).sample())
        }
    }
}


fun main() {
    val deps = declareDependencies {
        add { Clerk() }
        add { ComponentQueue<Customer>("waiting line") }
    }

    val env = createSimulation(true, dependencies = deps) {
        CustomerGenerator()
    }.run(50.0)

    val waitingLine: ComponentQueue<Customer> = env.get()

    waitingLine.stats.print()

    if(canDisplay()) {
        waitingLine.queueLengthTimeline.display()
        waitingLine.lengthOfStayTimeline.display()
    }
}
