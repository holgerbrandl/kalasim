//LogChannelConsumer.kts
package org.kalasim.examples.analysis

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kalasim.*


class MyEventConsumer : EventListener {
    // think of it as a  Non Blocking Queue
    val ordersChannel = Channel<Event>()

    override fun consume(event: Event) {
        GlobalScope.launch {
            ordersChannel.offer(event)
        }
    }
}

val tl = MyEventConsumer()

// Start a log consumer
GlobalScope.launch {
    tl.ordersChannel.receiveAsFlow().filter {
       it is InteractionEvent &&  it.curComponent?.name == "ComponentGenerator.1"
    }.collect {
        println(it)
    }
}

// create simulation with no default logging
val sim = createSimulation {
    ComponentGenerator(iat = 1.asDist()) { Component("Car.${it}") }

    // add custom log consumer
    addEventListener(tl)

    // run the simulation
    run(100)
}
