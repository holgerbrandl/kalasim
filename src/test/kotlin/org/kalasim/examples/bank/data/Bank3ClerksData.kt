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

            yield(hold(UniformRealDistribution(env.rg, 5.0, 15.0).sample()))
        }
    }
}

class Customer : Component()


class Clerk(val waitingLine: ComponentQueue<Customer>) : Component() {

    override fun process() = sequence {
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
        add { (1..3).map { Clerk(get()) } }
    }

    createSimulation(dependencies = deps) {
        run(50000.0)

        val waitingLine: ComponentQueue<Customer> = get()

        waitingLine.printStats()
    }
}
