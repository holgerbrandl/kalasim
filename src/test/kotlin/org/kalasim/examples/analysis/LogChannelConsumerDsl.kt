package org.kalasim.examples.analysis

import org.kalasim.Component
import org.kalasim.ComponentGenerator
import org.kalasim.InteractionEvent
import org.kalasim.asDist
import org.kalasim.createSimulation

// create simulation with no default logging
val sim = createSimulation {
    ComponentGenerator(iat = 1.asDist()) { Component("Car.${it}") }

    // add custom log consumer
    addEventListener<InteractionEvent> { event ->
        if (event.curComponent?.name == "ComponentGenerator.1")
            println(event)
    }

    // run the simulation
    run(100)
}