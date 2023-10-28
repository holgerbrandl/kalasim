//Bank3ClerksState.kt

package org.kalasim.examples.bank.state

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

class CustomerGenerator : Component() {

    override fun process() = sequence {
        while(true) {
            Customer(get(), get())
            hold(UniformRealDistribution(env.rg, 5.0, 15.0).minutes.sample())
        }
    }
}

class Customer(val workTodo: State<Boolean>, val waitingLine: ComponentQueue<Customer>) : Component() {
    override fun process() = sequence {
        waitingLine.add(this@Customer)
        workTodo.trigger(true, max = 1)
        passivate()
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()
    val workTodo: State<Boolean> by inject()

    override fun process() = sequence {
        while(true) {
            if(waitingLine.isEmpty())
                wait(workTodo, true)

            val customer = waitingLine.poll()

            hold(32.minutes) // bearbeitungszeit
            customer.activate()
        }
    }
}


fun main() {
    val env = declareDependencies {
        // register components needed for dependency injection
        dependency { ComponentQueue<Customer>("waitingline") }
        dependency { State(false, "worktodo") }

    }.createSimulation {
        enableComponentLogger()

        // register other components to  be present
        // when starting the simulation
        repeat(3) { Clerk() }
        CustomerGenerator()

    }

    env.run(500.0)

    println(env.get<ComponentQueue<Customer>>().statistics)
    env.get<State<Boolean>>().printSummary()

//    val waitingLine: ComponentQueue<Customer> = env.get()
//    waitingLine.stats.print()
//    waitingLine.queueLengthMonitor.display()
}
