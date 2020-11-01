@file:Suppress("MemberVisibilityCanBePrivate")

package org.github.holgerbrandl.kalasim.examples.kalasim

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.kalasim.*
import org.koin.core.get
import org.koin.core.inject


fun main() {

    class Customer(val workTodo: State<Boolean>, val waitingLine: ComponentQueue<Customer>) : Component() {
        override suspend fun SequenceScope<Component>.process(it: Component) {
            waitingLine.add(this@Customer)
            workTodo.trigger(max = 1, true)
            yield(passivate())
        }
    }

    class CustomerGenerator : Component() {

        override fun process() = sequence {
            while (true) {
                Customer(get(), get())
                yield(hold(UniformRealDistribution(5.0, 15.0).sample()))
            }
        }
    }

    class Clerk : Component() {
        val waitingLine: ComponentQueue<Customer> by inject()
        val workTodo: State<Any> by inject()

        override suspend fun SequenceScope<Component>.process() {
            while (waitingLine.isEmpty())
                yield(this@Clerk.wait(workTodo, true))

            val customer = waitingLine.poll()

            yield(hold(30.0)) // bearbeitungszeit
            customer.activate()
        }
    }

    val env = configureEnvironment {
        add { ComponentQueue<Customer>("waitingline") }
        add { State<Any>("worktodo") }

//        repeat(10) { add{ Customer(get(), get()) } }
        // register components needed for dependency injection
//        single(createdAtStart = true) { ComponentQueue<Customer>() }
//        single(createdAtStart = true) { Clerk() }

//        single { HelloServiceImpl(get()) as HelloService }
    }.apply {
        repeat(3) { Clerk() }

        // register other components by simpliy
        CustomerGenerator()
    }.run(50000.0)

    val waitingLine: ComponentQueue<Customer> = env.get()
    waitingLine.printStats()
}
