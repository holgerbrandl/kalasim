//Bank3ClerksRenegingState.kt
package org.kalasim.examples.bank.reneging_state


import org.kalasim.*
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.days


//to inject use data class Counter(var value: Int)
var numBalked: Int = 0
var numReneged: Int = 0


class Customer(val waitingLine: ComponentQueue<Customer>, val workToDo: State<Boolean>) : Component() {
    @Suppress("unused")
    private val clerks: List<Clerk> by inject()

    override fun process() = sequence {
        if(waitingLine.size >= 5) {
            numBalked++
            log("balked")
            cancel()
        }

        waitingLine.add(this@Customer)
        workToDo.trigger(true, false, max = 1)
        hold(50.minutes) // if not serviced within this time, renege

        if(waitingLine.contains(this@Customer)) {
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
        while(true) {
            if(waitingLine.isEmpty()) {
                wait(workToDo, true)
            }

            println("WAITLENGTH " + waitingLine.size)
            waitingLine.printSummary()

            val customer = waitingLine.poll()
            hold(30.minutes) // process customer

//            require(customer.isPassive){ "not passive"}
            customer.activate() // signal the customer that's all's done
        }
    }
}


fun main() {
    val env = createSimulation {
        enableComponentLogger()

        // register components needed for dependency injection
        dependency { ComponentQueue<Customer>("waitingline") }
        dependency { State(false, "worktodo") }
        dependency { (0..2).map { Clerk(get()) } }

        // register other components to be present when starting the simulation
        ComponentGenerator(iat = uniform(5.0, 15.0).minutes) {
            Customer(get(), get())
        }
    }


    val waitingLine: ComponentQueue<Customer> = env.get()
    val workToDo: State<Boolean> = env.get()

    env.run(3.days)

    // with kravis
//        waitingLine.queueLengthMonitor.display()
//        waitingLine.lengthOfStayMonitor.display()

    // with console
    waitingLine.printHistogram()
    workToDo.printHistograms()

    println("number reneged: $numReneged")
    println("number balked: $numBalked")
}
