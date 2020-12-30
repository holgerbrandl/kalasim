//Bank3ClerksData.kt
package org.kalasim.examples.bank.data


import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.component.get
import org.koin.core.component.inject


class CustomerGenerator(val waitingLine: ComponentQueue<Customer>) : Component() {
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        while (true) {
            waitingLine.add(Customer())

            for (clerk in clerks) {
                if (clerk.isPassive) {
                    clerk.activate()
                    break
                }
            }

            yield(hold(uniform( 5.0, 15.0,env.rg).sample()))
        }
    }
}

class Customer : Component()


class Clerk() : Component() {

    override fun process() = sequence {
        val waitingLine = get<ComponentQueue<Customer>>()

        while (true) {
            if (waitingLine.isEmpty())
                yield(passivate())

            waitingLine.poll() // returns next customer (value ignored here)
            yield(hold(30.0)) // bearbeitungszeit
        }
    }
}


fun main() {
    val deps = declareDependencies {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { CustomerGenerator(get()) }
        add { (1..3).map { Clerk() } }
    }

    createSimulation(dependencies = deps) {
        run(50000.0)

        val waitingLine: ComponentQueue<Customer> = get()

        waitingLine.printStats()
    }
}
