package org.kalasim.test

import org.kalasim.Component
import org.kalasim.createSimulation

fun main() {

    createSimulation {

        object : Component(){
            override fun process() = sequence {
                hold(10)
                activate()

            }
        }

        run(0)

        println("sim time after interruption is ${now}")
    }
}