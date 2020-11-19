@file:Suppress("MemberVisibilityCanBePrivate")

package org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.state

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.kalasim.*
import org.koin.core.get
import org.koin.core.inject

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

    override fun process() = sequence{
        if (waitingLine.isEmpty())
            yield(this@Clerk.wait(workTodo, true))

        val customer = waitingLine.poll()

        yield(hold(30.0)) // bearbeitungszeit
        customer.activate()
    }
}


fun main() {
    val env = configureEnvironment {
        // register components needed for dependency injection
        add { ComponentQueue<Customer>("waitingline") }
        add { State(false, "worktodo") }
    }.apply {
        // register other components to  be present when starting the simulation
        repeat(3) { Clerk() }
        CustomerGenerator()
    }.run(50000.0)

    env.get<ComponentQueue<Customer>>().printStats()
    env.get<State<Boolean>>().printInfo()
}
