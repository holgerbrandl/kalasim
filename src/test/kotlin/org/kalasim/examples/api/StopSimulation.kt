package org.kalasim.test

import org.kalasim.Component
import org.kalasim.createSimulation
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// Example that demonstrates how to stop a simulation. See https://www.kalasim.org/basics/ for reference
fun main() {

    createSimulation {

        object : Component() {
            override fun process() = sequence {
                hold(10.minutes, "something is about to happen")
                stopSimulation()
                hold(3.hours, "this ain't happening today")
            }
        }

        run() // try spinning the wheel until the queue runs dry

        println("sim time after interruption is $now")
    }
}