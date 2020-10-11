@file:Suppress("MemberVisibilityCanBePrivate")

package org.github.holgerbrandl.kalasim.examples.koiner

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.kalasim.Component
import org.github.holgerbrandl.kalasim.ComponentQueue
import org.github.holgerbrandl.kalasim.Environment
import org.github.holgerbrandl.kalasim.add
import org.koin.core.get
import org.koin.core.inject
import org.koin.dsl.module


class CustomerGenerator : Component() {

    //    var numCreated  = 0
    override fun process() = sequence {
//        if(numCreated++ >5 ) return@sequence
        while (true) {
            Customer(get(), get())

            yield(hold(UniformRealDistribution(5.0, 15.0).sample()))
        }
    }
}

class Customer(val waitingLine: ComponentQueue<Customer>, val clerk: Clerk) : Component() {

//    override fun process() = this.let {
//        sequence {
//            waitingLine.add(it)
//
//            if (clerk.isPassive) clerk.activate()
//
//            yield(passivate())
//        }
//    }

    override fun process() = sequence {
        while (true) {
            waitingLine.add(this@Customer)

            if (clerk.isPassive) clerk.activate()

            yield(passivate())
        }
    }
}

class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override suspend fun SequenceScope<Component>.process() {
        while (waitingLine.isEmpty()) yield(passivate())

        val customer = waitingLine.poll()

        yield(hold(30.0)) // bearbeitungszeit
        customer.activate()
    }
}

fun createSimulation(builder: org.koin.core.module.Module.() -> Unit): Environment =
    Environment(module(createdAtStart = true) { builder() })

fun main() {


    val env = createSimulation {

        add { Clerk() }
        add { ComponentQueue<Customer>() }

//        repeat(10) { add{ Customer(get(), get()) } }
        // register components needed for dependency injection
//        single(createdAtStart = true) { ComponentQueue<Customer>() }
//        single(createdAtStart = true) { Clerk() }

//        single { HelloServiceImpl(get()) as HelloService }
    }.apply {
        // register other components by simpliy
        CustomerGenerator()
    }.run(50.0)

    val waitingLine: ComponentQueue<Customer> = env.get()

//    waitingLine.stats.print()
}