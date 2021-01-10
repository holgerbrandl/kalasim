//DiningPhilosophers.kt
package org.kalasim.examples

import org.kalasim.*
import org.kalasim.misc.repeat


fun main() {
    class Fork : Resource()

    class Philosopher(val leftFork: Fork, val rightFork: Fork) : Component() {
        val thinking = exponential(1)
        val eating = exponential(1)


        override fun process() = sequence<Component> {
            while(true) {
                hold(thinking())
                request(leftFork) {
                    hold(0.1) // wait before taking the second fork
                    request(rightFork) {
                        hold(eating())
                        log("$name is eating")
                    }
                }
            }
        }
    }

    val env = createSimulation(true) {
        val tc = traceCollector()

        // create forks and resources
        val names = listOf("Socrates", "Pythagoras", "Plato", "Aristotle")
        val forks = repeat(names.size) { Fork() }.repeat().take(names.size + 1).toList()
        val philosophers = repeat(forks.size) { Philosopher(forks[it - 1], forks[it.rem(forks.size)]) }

        run(100)


        // Analysis

        // gather monitoring data (as in simmer:get_mon_arrivals)

        //        env.tc.traces.filter{ it.source is Resource}.map{ \}


    }
}