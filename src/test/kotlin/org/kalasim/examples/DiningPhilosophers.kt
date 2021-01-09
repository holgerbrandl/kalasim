package org.kalasim.examples

import org.kalasim.*
import org.kalasim.misc.repeat


fun main() {
    class Fork : Resource()

    class Philosopher(val leftFork: Fork, val rightFork: Fork): Component(){
        val thinking = exponential(1)
        val eating = exponential(1)

        override fun process() =sequence<Component> {
            while(true) {
                hold(thinking())
                request(leftFork) {
                    hold(0.1) // wait before taking the second fork
                    request(rightFork) {
                        hold(eating())
                        printTrace("$name is eating")
                    }
                }
            }
        }
    }

    val env = createSimulation(true) {
        // create forks and resources
        val forks = (1..4).map{ Fork()} //.repeat().take(100).toList()
        val philosophers = repeat(forks.size){
            Philosopher(forks[it-1], forks[it.rem(forks.size)])
        }

        run(100)
    }

    // Analysis

}