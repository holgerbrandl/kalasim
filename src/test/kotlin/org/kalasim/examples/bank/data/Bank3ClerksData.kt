//Bank3ClerksData.kt
package org.kalasim.examples.bank.data


import org.kalasim.*
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds


class CustomerGenerator(val waitingLine: ComponentQueue<Customer>) : Component() {
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        while(true) {
            waitingLine.add(Customer())

            for(clerk in clerks) {
                if(clerk.isPassive) {
                    clerk.activate()
                    break
                }
            }

            hold(uniform(5.0, 15.0).minutes.sample())
        }
    }
}

class Customer : Component()


class Clerk : Component() {

    override fun process() = sequence {
        val waitingLine = get<ComponentQueue<Customer>>()

        while(true) {
            if(waitingLine.isEmpty())
                passivate()

            waitingLine.poll() // returns next customer (value ignored here)
            hold(30.seconds) // bearbeitungszeit
        }
    }
}


fun main() {
    createSimulation {
        // register components needed for dependency injection
        dependency { ComponentQueue<Customer>("waitingline") }
        dependency { CustomerGenerator(get()) }
        dependency { (1..3).map { Clerk() } }

        run(50.days)

        val waitingLine: ComponentQueue<Customer> = get()

        println(waitingLine.statistics.toJson())
    }
}
