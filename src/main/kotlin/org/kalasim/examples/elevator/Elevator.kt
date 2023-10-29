//Elevator.kt
package org.kalasim.examples.elevator

import org.kalasim.*
import org.kalasim.animation.AnimationComponent
import org.kalasim.examples.elevator.Car.DoorState.CLOSED
import org.kalasim.examples.elevator.Car.DoorState.OPEN
import org.kalasim.examples.elevator.Direction.*
import org.kalasim.misc.repeat
import java.awt.Point
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

// For example documentation see https://www.kalasim.org/examples/office_tower/

val MOVE_TIME = 10.seconds
val DOOR_OPEN_TIME = 3.seconds
val DOOR_CLOSE_TIME = 3.seconds
val ENTER_TIME = 3.seconds
val EXIT_TIME = 3.seconds


enum class Direction {
    DOWN, STILL, UP;

    fun asIncrement(): Int = when(this) {
        DOWN -> -1
        STILL -> 0
        UP -> 1
    }

    fun invert(): Direction = when(this) {
        DOWN -> UP
        STILL -> throw IllegalArgumentException()
        UP -> DOWN
    }
}

private fun getDirection(from: Int, to: Int) = if(from < to) UP else DOWN


class VisitorGenerator(
    val fromRange: Pair<Int, Int>,
    val toRange: Pair<Int, Int>,
    val load: Int,
    name: String
) : TickedComponent(name) {

    val fromTo = sequence {
        val dintFrom = discreteUniform(fromRange.first, fromRange.second)
        val dintTo = discreteUniform(toRange.first, toRange.second)

        yieldAll(generateSequence { dintFrom() to dintTo() })
    }.filter { it.first != it.second }.iterator()

    override fun process() = sequence {
        while(true) {
            val (from, to) = fromTo.next()

            Visitor(from, to)

            if(load == 0) passivate()

            val iat = 3600 / load
            hold(uniform(0.5, 1.5).sample() * iat) // TODO should we hold before the first visitor?
        }
    }
}


// todo how to get instance number in super-constructor?
class Visitor(val from: Int, val to: Int) : Component() {

    val elevator = get<Elevator>()

    val fromFloor = elevator.floors[from]
    val toFloor = elevator.floors[to]
    val direction = getDirection(from, to)

    override fun toString() = "$name($from->$to)" // todo remove

    override fun process() = sequence {
        fromFloor.queue.add(this@Visitor)

        // call car if not yet done by another visitor waiting
        // on the same floor for the same direction
        get<Requests>().putIfAbsent(fromFloor to direction, env.now)

        elevator.cars.firstOrNull { it.isPassive }?.activate()

        passivate() // wait for it
    }
}


class Car(initialFloor: Floor, val capacity: Int) :
    AnimationComponent(Point(1, initialFloor.level)) {
    var direction = STILL
    var floor = initialFloor

    // todo why is this is queue (it does no seem to be used as such; stats?)
    val visitors = ComponentQueue<Visitor>("passengers of $name")

    enum class DoorState { OPEN, CLOSED }

    var door = CLOSED


    override fun process() = sequence {
        while(true) {
            val requests = get<Requests>()

            if(direction == STILL && requests.isEmpty()) passivate()

            val leaving = visitors.components.filter { it.toFloor == floor }

            if(leaving.isNotEmpty()) {
                log("Stopping on floor ${floor.level} because ${leaving.size} passenger want to get out")

                openDoor()
                leaving.forEach {
                    it.leave(visitors)
                    it.activate()
                }

                hold(EXIT_TIME, description = "Passengers exiting")
            }

            if(direction == STILL) direction = enumerated(UP, DOWN).sample()


            // https://kotlinlang.slack.com/archives/C0922A726/p1610700674029200
            run {
                // try to continue in the same direction or change
                for(dir in listOf(direction, direction.invert())) {
                    direction = dir

                    if(requests.containsKey(floor to direction)) {
                        requests.remove(floor to direction) // consume it right away so that other cars doors won't open as well

                        this@sequence.openDoor()

                        val zusteiger = floor.queue.components
                            .filter { it.direction == direction }
                            .take(capacity - visitors.size)

                        zusteiger.forEach {
                            it.leave(floor.queue)
                            visitors.add(it)
                        }

                        this@sequence.hold(
                            ENTER_TIME * zusteiger.size,
                            description = "${zusteiger.size} passengers entering"
                        )

                        // If there are still visitors going up/down in that floor
                        // then restore the request to the list of requests
                        val countInDirection = floor.queue.components.count { it.direction == direction }
                        if(countInDirection > 0) {
                            requests.putIfAbsent(floor to direction, env.now)
                        }
                    }

                    // if car is empty, than continue the loop to change direction if there are requests
                    if(visitors.isNotEmpty()) {
                        return@run
                    }
                }

                // loop terminated through exhaustion, i.e. there are passengers in the car
                direction = if(requests.isNotEmpty()) {
                    val earliestRequest = requests.minByOrNull { it.value }!!
                    // start in this direction
                    getDirection(floor.level, earliestRequest.key.first.level)
                } else {
                    STILL
                }
            }

            closeDoor()

            val floors = get<Elevator>().floors


            if(direction != STILL) {
                val nextFloor = floors[floors.indexOf(floor) + direction.asIncrement()]
//                hold(MOVE_TIME, description = "Moving to ${nextFloor.level}")
                move(Point(0, nextFloor.level), description = "Moving to ${nextFloor.level}", speed = 0.1)

                floor = nextFloor
            }
        }
    }

    suspend fun SequenceScope<Component>.openDoor() {
        if(door == OPEN) return
        hold(DOOR_OPEN_TIME, description = "Opening door of ${this@Car.name}")
        door = OPEN
    }

    suspend fun SequenceScope<Component>.closeDoor() {
        if(door == CLOSED) return
        hold(DOOR_CLOSE_TIME, description = "Closing door of ${this@Car.name}")
        door = CLOSED
    }
}

class Elevator(
    showLog: Boolean = false,
    load0N: Int = 50,
    loadNN: Int = 100,
    loadN0: Int = 100,
    carCapacity: Int = 4,
    numCars: Int = 3,
    topFloor: Int = 15,
) : Environment(enableComponentLogger = showLog, tickDurationUnit = DurationUnit.MINUTES) {
    init {
        dependency { this@Elevator }
    }

    val floors = repeat(1 + topFloor) { Floor(it - 1) }
    val cars = repeat(numCars) { Car(floors.first(), carCapacity) }

    val requests: Requests = dependency { mutableMapOf() }

    init {
        VisitorGenerator(0 to 0, 1 to topFloor, load0N, "vg_0_n")
        VisitorGenerator(1 to topFloor, 0 to 0, loadN0, "vg_n_0")
        VisitorGenerator(1 to topFloor, 1 to topFloor, loadNN, "vg_n_n")
    }
}


class Floor(val level: Int, val queue: ComponentQueue<Visitor> = ComponentQueue()) {
    override fun toString(): String {
        return "Floor($level)"
    }
}

typealias Requests = MutableMap<Pair<Floor, Direction>, SimTime>


fun main() {
    val elevator = Elevator()

    elevator.run(2.days)

    println(elevator.cars[0].visitors.statistics.lengthOfStayStats)
}