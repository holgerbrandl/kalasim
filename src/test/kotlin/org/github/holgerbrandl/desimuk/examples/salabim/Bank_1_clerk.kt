package org.github.holgerbrandl.desimuk.examples.koiner

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.desimuk.Component
import org.github.holgerbrandl.desimuk.ComponentQueue
import org.github.holgerbrandl.desimuk.Environment
import org.koin.core.get
import org.koin.core.inject
import org.koin.dsl.module


class CustomerGenerator : Component() {

    override fun process() = sequence {
        Customer(get(), get())

        yield(hold(UniformRealDistribution(5.0, 15.0).sample()))
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
        waitingLine.add(this@Customer)

        if (clerk.isPassive) clerk.activate()

        yield(passivate())
    }
}

class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override suspend fun SequenceScope<Component>.process() {
        while (waitingLine.isEmpty()) yield(passivate())

        val customer = waitingLine.poll() as Customer

        yield(hold(30.0)) // bearbeitungszeit
        customer.activate()
    }
}

fun createSimulation(builder: org.koin.core.module.Module.() -> Unit): Environment = Environment(module { builder() })

fun main() {

    createSimulation {

        // register components needed for injection
        single { ComponentQueue<Customer>() }
        single { Clerk() }

//        single { HelloServiceImpl(get()) as HelloService }
    }.apply {
        // register other components
        CustomerGenerator()
    }.run( 100.0 )
}
