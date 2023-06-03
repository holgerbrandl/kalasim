//Bank3ClerksRenegingState.kt
package org.kalasim.examples.bank.reneging_state


import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.component.inject


//to inject use data class Counter(var value: Int)
var numBalked: Int = 0
var numReneged: Int = 0


class Customer(val waitingLine: ComponentQueue<Customer>, val workToDo: State<Boolean>) : Component() {
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        if (waitingLine.size >= 5) {
            numBalked++
            log("balked")
            cancel()
        }

        waitingLine.add(this@Customer)
        workToDo.trigger(true, false, max = 1)
        hold(50) // if not serviced within this time, renege

        if (waitingLine.contains(this@Customer)) {
            waitingLine.remove(this@Customer)
            numReneged++
            log("reneged")

        } else {
            passivate()
        }
    }
}


class Clerk(val workToDo: State<Boolean>) : Component() {
    private val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while (true) {
            if (waitingLine.isEmpty()) {
                wait(workToDo, true)
            }

            println("WAITLENGTH " + waitingLine.size)
            waitingLine.printSummary()

            val customer = waitingLine.poll()
            hold(30.0) // process customer

//            require(customer.isPassive){ "not passive"}
            customer.activate() // signal the customer that's all's done
        }
    }
}


fun main() {
    val env = configureEnvironment(true) {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { State(false, "worktodo") }
        add { (0..2).map { Clerk(get()) } }
    }

    // register other components to  be present when starting the simulation
    ComponentGenerator(iat = UniformRealDistribution(env.rg, 5.0, 15.0)) {
        val customer = Customer(get(), get())
        customer
    }

    val waitingLine: ComponentQueue<Customer> = env.get()
    val workToDo: State<Boolean> = env.get()

    env.run(until = 50000.toTickTime())

    // with kravis
//        waitingLine.queueLengthMonitor.display()
//        waitingLine.lengthOfStayMonitor.display()

    // with console
    waitingLine.printHistogram()
    workToDo.printHistograms()

    println("number reneged: $numReneged")
    println("number balked: $numBalked")
}
