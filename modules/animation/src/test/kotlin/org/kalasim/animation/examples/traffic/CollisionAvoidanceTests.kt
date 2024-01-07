package org.kalasim.animation.examples.traffic

import org.junit.Assert
import org.kalasim.animation.*
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
        val road = generateRoadWithBuildings(numSegments = 2, numBuildings = 2)

        val roadDict = road.segments2buildings()

        // create a faster car
        road.addCar(object : Vehicle(roadDict[0].second[0].port, "car1") {
            override fun process() = sequence {
                hold(2.seconds)
                moveTo(roadDict[1].second[0].port)
            }
        })

        // create a slower car on the same road to trigger a collision situation
        road.addCar(object : Vehicle(roadDict[0].second[1].port, "car2", maxSpeed = 30.kmh) {
            override fun process() = sequence {
                hold(5.seconds)
                moveTo(roadDict[1].second[1].port)
            }
        })

        // enable collision sampler (only to fail fast)
        CollisionSampler(100.milliseconds)

//        animateCrossing { road }

        road.run(3.hours)

        road.cars[0].currentPosition shouldBeInCloseProximityOf roadDict[1].second[0].port.position
        road.cars[1].currentPosition shouldBeInCloseProximityOf roadDict[1].second[1].port.position
    }


    @Test
    fun `car must wait until road ahead is empty with crossing`() = createTestSimulation {
        val road = generateRoadWithBuildings(numSegments = 2, numBuildings = 2)

        val roadDict = road.segments2buildings()

        // create a faster car
        road.addCar(object : Vehicle(roadDict[0].second[0].port, "car1", maxSpeed = 150.kmh) {
            override fun process() = sequence {
                hold(3.seconds)
                moveTo(roadDict[1].second[0].port)
            }
        })

        // create a slower car on the same road to trigger a collision situation
        road.addCar(object : Vehicle(roadDict[0].second[1].port, "car2", maxSpeed = 5.kmh) {
            override fun process() = sequence {
                hold(2.seconds)
                moveTo(roadDict[1].second[1].port)
            }
        })

        // enable collision sampler (only to fail fast)
        CollisionSampler(100.milliseconds)

//        animateCrossing { road }

        road.run(3.hours)

        road.cars[0].currentPosition shouldBeInCloseProximityOf roadDict[1].second[0].port.position
        road.cars[1].currentPosition shouldBeInCloseProximityOf roadDict[1].second[1].port.position
    }


    @Test
    fun `car must stop and go to avoid collision `() = createTestSimulation {
        val segment = PathSegment("seg_start", Node("n_start", Point(0, 0)), Node("n_end", Point(0, 100)))

        val buildings = List(4) {
            Building("b${it}", Port("p${it}", 0.1 + 0.2 * it, segment))
        }

        val cityMap = CityMap(listOf(segment), buildings)
        val road = Crossing(cityMap)

        val roadDict = road.segments2buildings()

        // create a faster car
        road.addCar(object : Vehicle(roadDict[0].second[0].port, "fast_car", maxSpeed = 150.kmh) {
            override fun process() = sequence {
                hold(2.seconds)
                moveTo(roadDict[0].second[2].port)
            }
        })

        // create a slower car on the same road to trigger a collision situation
        road.addCar(object : Vehicle(roadDict[0].second[1].port, "slow_car", maxSpeed = 5.kmh) {
            override fun process() = sequence {
                hold(10.seconds)
                moveTo(roadDict[0].second[3].port)
            }
        })

        // enable collision sampler (only to fail fast)
        CollisionSampler(100.milliseconds)

//        animateCrossing { road }

        road.run(3.hours)

        road.cars[0].currentPosition shouldBeInCloseProximityOf roadDict[0].second[2].port.position
        road.cars[1].currentPosition shouldBeInCloseProximityOf roadDict[0].second[3].port.position
    }
}

infix fun Point.shouldBeInCloseProximityOf(position: Point) {
    Assert.assertTrue(this.distanceTo(position) < 0.1.meters);
}
