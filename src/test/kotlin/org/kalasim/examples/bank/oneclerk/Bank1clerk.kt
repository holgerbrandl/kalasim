//Bank1clerk.kt
@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.examples.bank.oneclerk

import org.kalasim.*
import org.kalasim.analytics.display
import org.koin.core.component.get
import org.koin.core.component.inject


class Customer(
    val waitingLine: ComponentQueue<Customer>,
    val clerk: Clerk
) : Component() {
    override fun process() = sequence {
        waitingLine.add(this@Customer)

        if (clerk.isPassive) clerk.activate()

        yield(passivate())
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override suspend fun SequenceScope<Component>.process() {
        while (true) {
            while (waitingLine.isEmpty()) yield(passivate())

            val customer = waitingLine.poll()

            yield(hold(10.0)) // bearbeitungszeit
            customer.activate()
        }
    }
}

class CustomerGenerator : Component() {

    //    var numCreated  = 0
    override fun process() = sequence {
//        if(numCreated++ >5 ) return@sequence
        while (true) {
            Customer(get(), get())

            yield(hold(uniform(5.0, 15.0).sample()))
        }
    }
}


fun main() {
    val deps = declareDependencies {
        add { Clerk() }
        add { ComponentQueue<Customer>("waiting line") }
    }

    val env = createSimulation(dependencies = deps) {
        CustomerGenerator()
    }.run(50.0)

    val waitingLine: ComponentQueue<Customer> = env.get()

    waitingLine.stats.print()
    waitingLine.queueLengthMonitor.display()
    waitingLine.lengthOfStayMonitor.display()
}
