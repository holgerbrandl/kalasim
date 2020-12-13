package org.kalasim.examples.bank.threeclerks


import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.kalasim.analytics.display
import org.koin.core.component.get
import org.koin.core.component.inject
import java.awt.GraphicsEnvironment.isHeadless


class CustomerGenerator : Component() {

    override fun process() = sequence {
        while (true) {
            Customer(get())
            yield(hold(UniformRealDistribution(env.rg, 5.0, 15.0).sample()))
        }
    }
}

class Customer(val waitingLine: ComponentQueue<Customer>) : Component() {
    private val clerks: List<Clerk> by inject()

    override suspend fun SequenceScope<Component>.process(it: Component) {
        waitingLine.add(this@Customer)

        for (c in clerks) {
            if (c.isPassive) {
                c.activate()
                break // activate only one clerk
            }
        }

        yield(passivate())
    }
}


class Clerk : Component() {
    private val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while (true) {
            if (waitingLine.isEmpty())
                yield(passivate())

            val customer = waitingLine.poll()
            yield(hold(30.0)) // bearbeitungszeit
            customer.activate() // signal the customer that's all's done
        }
    }
}


fun main() {
    val env = configureEnvironment {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { State(false, "worktodo") }
        add { CustomerGenerator() }
        add { (1..3).map { Clerk() } }
    }

    env.apply {
        // register other components to  be present when starting the simulation
//        CustomerGenerator()

        run(50000.0)

        val waitingLine: ComponentQueue<Customer> = get()

        if(!isHeadless())
//        waitingLine.lengthOfStayMonitor.printHistogram()
//        waitingLine.queueLengthMonitor.printHistogram()

        waitingLine.queueLengthMonitor.display()
        waitingLine.lengthOfStayMonitor.display()

//        waitingLine.stats.toJson().toString(2).printThis()
        waitingLine.printInfo()
    }
}
