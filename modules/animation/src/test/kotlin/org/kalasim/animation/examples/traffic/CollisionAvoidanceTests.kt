package org.kalasim.animation.examples.traffic

import org.kalasim.animation.kmh
import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger
import org.kalasim.logistics.*
import org.kalasim.misc.createTestSimulation
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CollisionAvoidanceTests {

    @Test
    fun `cars must not crash`() {
        createSimulation {
            enableComponentLogger()

            val crossing = Crossing(2, 2, 2, 10)

            CollisionSampler()

            crossing.run(10.days)
        }
    }


    @Test
    fun `car must wait until road ahead is empty`() = createTestSimulation {
        val road = generateRoad(numSegments = 2, numBuildings = 2)

        val roadDict = road.segments2buildings()

        // create a faster car
        road.addCar(object : Vehicle(roadDict[0].second[0].port, "car1") {
            override fun process() = sequence {
                hold(2.seconds)
                yieldAll(moveTo(roadDict[1].second[0].port))
            }
        })

        // create a slower car on the same road to trigger a collision situation
        road.addCar(object : Vehicle(roadDict[0].second[1].port, "car2", maxSpeed = 30.kmh) {
            override fun process() = sequence {
                hold(5.seconds)
                yieldAll(moveTo(roadDict[1].second[1].port))
            }
        })

        // enable collision sampler (only to fail fast)
        CollisionSampler(100.milliseconds)

//            animateCrossing { road }

        road.run(3.hours)
    }


    @Test
    fun `car must wait until road ahead is empty simply`() = createTestSimulation {
        val road = generateRoad(numSegments = 2, numBuildings = 2)

        val roadDict = road.segments2buildings()

        // create a faster car
        road.addCar(object : Vehicle(roadDict[0].second[0].port, "car1", maxSpeed = 150.kmh) {
            override fun process() = sequence {
                hold(3.seconds)
                yieldAll(moveTo(roadDict[1].second[0].port))
            }
        })

        // create a slower car on the same road to trigger a collision situation
        road.addCar(object : Vehicle(roadDict[0].second[1].port, "car2", maxSpeed = 5.kmh) {
            override fun process() = sequence {
                hold(2.seconds)
                yieldAll(moveTo(roadDict[1].second[1].port))
            }
        })

        // enable collision sampler (only to fail fast)
//        CollisionSampler(100.milliseconds)

        animateCrossing { road }

        road.run(3.hours)
    }
}
