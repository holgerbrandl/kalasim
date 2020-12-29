//Bank3ClerksStandby.kt
@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.examples.bank.standby

//TODO

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.component.get
import org.koin.core.component.inject

class CustomerGenerator : Component() {

    override fun process() = sequence {
        while (true) {
            Customer(get(), get())
            yield(hold(UniformRealDistribution(env.rg, 5.0, 15.0).sample()))
        }
    }
}

class Customer(val workTodo: State<Boolean>, val waitingLine: ComponentQueue<Customer>) : Component() {
    override suspend fun SequenceScope<Component>.process(it: Component) {
        waitingLine.add(this@Customer)
        workTodo.trigger(true, max = 1)
        yield(passivate())
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()
    val workTodo: State<Boolean> by inject()

    override fun process() = sequence {
        while (true) {
            if (waitingLine.isEmpty())
                yield(this@Clerk.wait(workTodo, true))

            val customer = waitingLine.poll()

            yield(hold(32.0)) // bearbeitungszeit
            customer.activate()
        }
    }
}


fun main() {
    val env = configureEnvironment(true) {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { State(false, "worktodo") }
    }.apply {
        // register other components to  be present when starting the simulation
        repeat(3) { Clerk() }
        CustomerGenerator()
    }.run(500.0)

    env.get<ComponentQueue<Customer>>().printStats()
    env.get<State<Boolean>>().printInfo()

//    val waitingLine: ComponentQueue<Customer> = env.get()
//    waitingLine.stats.print()
//    waitingLine.queueLengthMonitor.display()
}
