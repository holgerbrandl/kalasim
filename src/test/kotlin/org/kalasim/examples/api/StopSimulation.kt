package org.kalasim.test

import org.kalasim.Component
import org.kalasim.createSimulation

// Example that demonstrates how to stop a simulation. See https://www.kalasim.org/basics/ for reference
fun main() {

    createSimulation {

        object : Component(){
            override fun process() = sequence {
                hold(10, "something is about to happen")
                stopSimulation()
                hold(10, "this ain't happening today")
            }
        }

        run() // try spinning the wheel until the queue runs dry

        println("sim time after interruption is $now")
    }
}