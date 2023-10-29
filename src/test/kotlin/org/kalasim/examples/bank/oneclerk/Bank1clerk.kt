//Bank1clerk.kt
package org.kalasim.examples.bank.oneclerk

import org.kalasim.*
import org.kalasim.misc.printThis
import org.kalasim.plot.kravis.canDisplay
import org.kalasim.plot.kravis.display
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


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


class Clerk(val serviceTime: Duration = 10.minutes) : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while(true) {
            while(waitingLine.isEmpty()) passivate()

            val customer = waitingLine.poll()

            hold(serviceTime) // bearbeitungszeit
            customer.activate()
        }
    }
}

class CustomerGenerator : Component() {

    override fun process() = sequence {
        while(true) {
            Customer(get(), get())

            hold(uniform(5.minutes, 15.minutes).sample())
        }
    }
}


fun main() {
    val deps = declareDependencies {
        dependency { Clerk() }
        dependency { ComponentQueue<Customer>("waiting line") }
    }

    val env = createSimulation(dependencies = deps) {
        enableComponentLogger()

        CustomerGenerator()
    }

    env.run(50.0)

    val waitingLine: ComponentQueue<Customer> = env.get()

    waitingLine.statistics.printThis()

    if(canDisplay()) {
        waitingLine.queueLengthTimeline.display()
        waitingLine.lengthOfStayStatistics.display()
    }
}
