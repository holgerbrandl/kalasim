package org.kalasim.test

import org.kalasim.Component
import org.kalasim.createSimulation

createSimulation {

    object : Component() {
        override fun process() = sequence {
            hold(10)
            activate()

        }
    }

    run()

    println("sim time after interruption is $now")
}