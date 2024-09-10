package org.kalasim.animation.examples.traffic

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Disabled
import org.kalasim.animation.Point
import org.kalasim.animation.kmh
import org.kalasim.enableComponentLogger
import org.kalasim.logistics.*
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class PathFindingTests {

    @Test
    fun `it should turn at the next crossing`() {

        val road = generateRoad(numSegments = 2)

        val ports = listOf(
            Port("A", 0.7, road[0], PortConnectivity.Forward),
            Port("B", 0.3, road[0], PortConnectivity.Forward)
        )
        val crossing = Crossing(CityMap.fromPorts(road, ports))

        with(crossing) {
            enableComponentLogger()

            // so to get from A to B is must do 2 turns at the dead ends.
            val car = object : Vehicle(ports[0], "car1", maxSpeed = 50.kmh) {
                override fun process() = sequence {
                    hold(6.seconds)
                    moveTo(ports[1])
                }
            }
            crossing.cars += car

            // first, validate the path itself
            val path = get<PathFinder>().findPath(car.currentPort!!, ports[1])
            car.activate(Vehicle::moveTo, ports[0])

            path.route.size shouldBe 3 // with 3 it turns at the next crossing

//            animateCrossing { crossing }
            crossing.run(1.days)

            car.currentPosition shouldBeInProximityOf ports[0].position
        }
    }


    @Disabled
    @Test
    fun `it should turn drive a circle in a unidirectional network`() {
        val roads = run {
            val n00 = Node("00", Point(0, 0))
            val n01 = Node("01", Point(0, 100))
            val n11 = Node("11", Point(100, 100))
            val n10 = Node("10", Point(100, 0))

            listOf(
                PathSegment("1", n00, n10, bidirectional = false),
                PathSegment("2", n10, n11, bidirectional = false),
                PathSegment("2", n11, n01, bidirectional = false),
                PathSegment("2", n01, n00, bidirectional = false)
            )
        }

        val ports = listOf(
            Port("A", 0.7, roads[0], PortConnectivity.Forward),
            Port("B", 0.3, roads[0], PortConnectivity.Forward)
        )

        val builder = fun(): Crossing {
            return Crossing(CityMap.fromPorts(roads, ports)).apply {
                cars += object : Vehicle(ports[0], maxSpeed = 50.kmh) {
                    override fun process() = sequence {
                        hold(6.seconds)
                        moveTo(ports[1])
                    }
                }
            }
        }

        // // just needed for visual debugging
        // animateCrossing (builder)

        val crossing = builder()
        val car = crossing.cars[0]
        val path = crossing.get<PathFinder>().findPath(car.currentPort!!, ports[1])

        path.route.size shouldBe 5 // with 3 it turns at the next crossing

        @Suppress("ControlFlowWithEmptyBody", "ConstantConditionIf", "ConstantConditionIf")
        if(false) { // just included to make sure that moveTo can be called outside suspend context
            car.activate(Vehicle::moveTo, ports[0])
        }

        crossing.run(1.days)

        car.currentPosition shouldBeInProximityOf ports[0].position
    }
}
