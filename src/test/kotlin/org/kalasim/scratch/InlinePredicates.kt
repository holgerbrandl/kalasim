package org.kalasim.scratch

import org.kalasim.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// Objective:
// dream to just say something like
// yield wait(car.direction == 'left' and weather='good')


fun main() {
    createSimulation {
        enableComponentLogger()

        val waiter = object : Component() {
            var isBusy = true

            override fun process() =
                sequence {
                    while(true) {
                        isBusy = !isBusy
                        hold(3.minutes)
                    }
                }
        }

        val customer = object : Component() {
            override fun process() =
                sequence {
                    waitPredicate { !waiter.isBusy }
                }

            suspend fun SequenceScope<Component>.waitPredicate(predicate: () -> Boolean) {
                while(!predicate()) standby()
            }
        }

        run(10.hours)
    }
}