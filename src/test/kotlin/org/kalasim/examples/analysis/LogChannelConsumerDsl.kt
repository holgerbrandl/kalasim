package org.kalasim.examples.analysis

import org.kalasim.Component
import org.kalasim.ComponentGenerator
import org.kalasim.asDist
import org.kalasim.createSimulation
import org.kalasim.misc.asyncEventListener

// create simulation with no default logging
val sim = createSimulation {
    ComponentGenerator(iat = 1.asDist()) { Component("Car.${it}") }

    // add custom log consumer
    addEventListener(
        asyncEventListener {
            onInteractionEvent { event ->
                if (event.curComponent?.name == "ComponentGenerator.1")
                    println(event)
            }
        })

    // run the simulation
    run(100)
}