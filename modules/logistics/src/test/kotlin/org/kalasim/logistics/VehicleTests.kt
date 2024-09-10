package org.kalasim.logistics

import org.junit.jupiter.api.Disabled
import org.kalasim.*
import org.kalasim.logistics.examples.simpleCrossing
import kotlin.test.Test


class VehicleTests {

    @Test
    @Disabled //TODO bring back for 2014.1
    fun `it should respect the right of way`() {
        createSimulation {
            enableComponentLogger()

            val map = simpleCrossing()

            class Car(startingPosition: Port) : Vehicle(startingPosition)

            dependency { PathFinder(map) }

//            val startTime = uniform(0, 10).minutes

            val cars = List(5) {
                Car(map.ports.random(random))
            }

            repeat(10) {
                cars.forEach {
                    it.activate(Vehicle::moveTo, map.ports.random(random))
                }
            }


            run(1.hour)
        }
    }
}