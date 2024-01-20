package org.kalasim.animation.examples.traffic

import org.junit.Assert
import org.kalasim.*
import org.kalasim.animation.*
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

        road.cars[0].currentPosition shouldBeInProximityOf roadDict[1].second[0].port.position
        road.cars[1].currentPosition shouldBeInProximityOf roadDict[1].second[1].port.position
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

        road.cars[0].currentPosition shouldBeInProximityOf roadDict[1].second[0].port.position
        road.cars[1].currentPosition shouldBeInProximityOf roadDict[1].second[1].port.position
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

        road.cars[0].currentPosition shouldBeInProximityOf roadDict[0].second[2].port.position
        road.cars[1].currentPosition shouldBeInProximityOf roadDict[0].second[3].port.position
    }


    @Test
    fun `vehicles must account for other vehicles entering and leaving the road`() = createTestSimulation {
        val segment = PathSegment("p_start", Node("p_end", Point(0, 0)), Node("n_end", Point(0, 1000)))

        val buildings = List(6) {
            Building("b${it}", Port("p${it}", 0.2 + 0.1 * it, segment))
        }

        val crossing = Crossing(CityMap(listOf(segment), buildings))

        val car1 = object : Vehicle(buildings[0].port, "car", maxSpeed = 80.kmh) { //25m/s

            override fun process() = sequence {
                hold(5.seconds)
                moveTo(buildings[5].port)
            }
        }

        val exited = State(false)

        val car2 = object : Vehicle(buildings[1].port, "exit_car", maxSpeed = 20.kmh) {
            override fun process() = sequence {
                hold(5.seconds)
                moveTo(buildings[2].port)
                hold(3.seconds)
                exitNetwork()
                exited.value = true
            }
        }

        val car3 = object : Vehicle(buildings[3].port, "enter_car", maxSpeed = 30.kmh) {
            override fun process() = sequence {
                wait(exited, true)
                hold(5.seconds)

                enterNetwork(buildings[3].port)
                hold(20.seconds) // wait until follow v

                moveTo(buildings[4].port)
            }
        }

        crossing.cars += listOf(car1, car2, car3)

//        CollisionSampler(100.milliseconds)

        object : Component() {
            override fun process() = sequence<Component> {
                val initPosC1 = car1.currentPosition
                val initPosC2 = car2.currentPosition
                val initPosC3 = car3.currentPosition
                hold(5.seconds)
                car2 shouldBeInProximityOf initPosC2

                hold(10.seconds)
                car1 shouldBeNotInProximityOf initPosC1
                car2 shouldBeNotInProximityOf initPosC2


                hold(20.seconds)
                car2 shouldBeInProximityOf buildings[2]
            }
        }

        animateCrossing { crossing }

//        crossing.run()
//        car1.currentPosition shouldBeInCloseProximityOf buildings[2].port.position
//        car2.currentPosition shouldBeInCloseProximityOf buildings[4].port.position
//        car3.currentPosition shouldBeInCloseProximityOf buildings[5].port.position
    }
}

infix fun Point.shouldBeInProximityOf(position: Point) = Assert.assertTrue(this.distanceTo(position) < 0.1.meters)
infix fun Vehicle.shouldBeInProximityOf(position: Point) = currentPosition shouldBeInProximityOf position
infix fun Vehicle.shouldBeInProximityOf(building: Building) =
    currentPosition shouldBeInProximityOf building.port.position

infix fun Point.shouldBeNotInProximityOf(position: Point) = Assert.assertTrue(this.distanceTo(position) < 0.1.meters)
infix fun Vehicle.shouldBeNotInProximityOf(position: Point) = currentPosition shouldBeNotInProximityOf position
infix fun Vehicle.shouldBeNotInProximityOf(building: Building) =
    currentPosition shouldBeNotInProximityOf building.port.position
