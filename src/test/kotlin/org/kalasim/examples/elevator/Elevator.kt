package org.kalasim.examples.elevator

import org.kalasim.*
import org.kalasim.examples.elevator.Car.DoorState.*
import org.kalasim.examples.elevator.Direction.*
import org.kalasim.misc.repeat
import java.util.*

// Adopted from https://github.com/salabim/salabim/blob/master/sample%20models/Elevator.py

// todo built second version where vistors get angry and walk away
// todo use inline class for better typing // inline class Floor(level: Int)

const val MOVE_TIME = 10
const val DOOR_OPEN_TIME = 3
const val DOOR_CLOSE_TIME = 3
const val ENTER_TIME = 3
const val EXIT_TIME = 3

const val LOAD_0_n = 50
const val LOAD_n_n = 100
const val LOAD_n_0 = 100
const val CAR_CAPACITY = 4
const val NUM_CARS = 3
const val TOP_FLOOR = 15

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

class VisitorGenerator(
    val fromRange: Pair<Int, Int>,
    val toRange: Pair<Int, Int>,
    val load: Int,
    name: String
) : Component(name) {

    val fromTo = sequence {
        val dintFrom = discreteUniform(fromRange.first, fromRange.second)
        val dintTo = discreteUniform(toRange.first, toRange.second)

        yieldAll(generateSequence { dintFrom() to dintTo() })
    }.filter { it.first != it.second }.iterator()

    override fun process() = sequence<Component> {
        while(true) {
            val (from, to) = fromTo.next()

            Visitor(from, to)

            if(load == 0) passivate()

            val iat = 3600 / load
            hold(uniform(0.5, 1.5).sample() * iat) // TODO should we hold before the first visitor
        }
    }
}

private fun getDirection(from: Int, to: Int) = if(from < to) UP else DOWN

class Visitor(from: Int, to: Int) : Component() {

    val building = get<Building>()

    val fromFloor = building.floors[from]
    val toFloor = building.floors[to]
    val direction = getDirection(from, to)

    override fun process() = sequence {
        fromFloor.visitors.add(this@Visitor)

        // call car if not yet done by another visitor waiting
        // on the same floor for the same direction
        get<Requests>().putIfAbsent(fromFloor to direction, env.now)

        building.cars.firstOrNull { it.isPassive }?.activate()

        passivate() // wait for it
    }
}


class Car(initialFloor: Floor, val capacity: Int = CAR_CAPACITY) : Component() {
    var direction = STILL
    var floor = initialFloor

    // todo why is this is queue (it does no seem to be used as such; stats?)
    val visitors = ComponentQueue<Visitor>("visitors in $name")

    enum class DoorState { OPEN, CLOSED }

    var door = CLOSED


    override fun process() = sequence {
        while(true) {
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

//            if(direction == STILL) direction  = listOf(UP, DOWN).random() // todo not deterministic
            if(direction == STILL) direction = enumerated(UP, DOWN).sample()

            val requests = get<Requests>()

            // try to continue in the same direction or change
            // todo rather use iterator approach for better control logic
            val directions = listOf(direction, direction.invert()).iterator()
            for(dir in directions) {
                if(requests.containsKey(floor to dir)) {
                    requests.remove(floor to dir) // consume it right away so that other cars' doors wont open as well

                    openDoor()

                    val zusteiger = floor.visitors.components
                        .filter { it.direction == dir }
                        .take(capacity - visitors.size)

                    zusteiger.forEach {
                        it.leave(floor.visitors)
                        visitors.add(it)
                    }
                    hold(ENTER_TIME * zusteiger.size, description = "Letting ${zusteiger.size} new passengers in")

                    // If there are still visitors going up/down in that floor
                    // then restore the request to the list of requests
                    val countInDirection = visitors.components.count { it.direction == dir }
                    if(countInDirection > 0) {
                        requests.putIfAbsent(floor to direction, env.now)
                    }
                }

                // if car is empty, than continue the loop to change direction if there are requests
                if(visitors.isNotEmpty()) {
                    break
                }
            }

            // https://kotlinlang.slack.com/archives/C0922A726/p1610700674029200
            if(!directions.hasNext()) { // loop terminated through exhaustion, i.e. there are passengers in the car
                direction = if(requests.isNotEmpty()) {
                    val earliestRequest = requests.minByOrNull { it.value }!!
                    // start in this direction
                    getDirection(floor.level, earliestRequest.key.first.level)
                } else {
                    STILL
                }
            }

            closeDoor()

            val floors = get<Building>().floors

            if(direction != STILL) {
                val nextFloor = floors[floors.indexOf(floor) + direction.asIncrement()-1]
                hold(MOVE_TIME)
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

class Building {
    val floors = repeat(1 + TOP_FLOOR) { Floor(it - 1) }
    val cars = repeat(NUM_CARS) { Car(floors.first()) }
}


class Floor(val level: Int, val visitors: ComponentQueue<Visitor> = ComponentQueue())

typealias Requests = MutableMap<Pair<Floor, Direction>, Double>

fun main() {

    createSimulation(true) {
        val building = dependency { Building() }

        val requests: Requests = mutableMapOf()
        dependency { requests }

        VisitorGenerator(0 to 0, 1 to TOP_FLOOR, LOAD_0_n, "vg_0_n")
        VisitorGenerator(1 to TOP_FLOOR, 0 to 0, LOAD_n_0, "vg_n_0")
        VisitorGenerator(1 to TOP_FLOOR, 1 to TOP_FLOOR, LOAD_n_n, "vg_n_nn")

        run(1000)


        //print summary
        println("Floor    n         Length Length_of_stay")
        building.floors.forEach {
            it.let {
                println(
                    "%5d, %5d, %15.3f : %15.3f".format(
                        it.level,
                        it.visitors.lengthOfStayMonitor.statistics().n,
                        it.visitors.queueLengthMonitor.statistics().mean,
                        it.visitors.lengthOfStayMonitor.statistics().mean
                    )
                )
            }
        }
    }
}