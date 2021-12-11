package org.kalasim.scratch

import org.kalasim.Component
import org.kalasim.createSimulation

// Objective:
// dream to just say something like
// yield wait(car.direction == 'left' and weather='good')


fun main() {
    createSimulation(true) {

        val waiter = object : Component() {

            var isBusy = true

            override fun process() =
                sequence {
                    while (true) {
                        isBusy = !isBusy
                        hold(3)
                    }
                }
        }

        val customer = object : Component() {
            override fun process() =
                sequence {
                    waitPredicate { !waiter.isBusy }
                }

            suspend fun SequenceScope<Component>.waitPredicate(predicate: () -> Boolean) {
                while (!predicate()) standby()
            }
        }

        run(10)
    }
}