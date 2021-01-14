package org.kalasim.examples.elevator

import org.kalasim.*
import org.kalasim.examples.elevator.Car.DoorState.*
import org.kalasim.examples.elevator.Direction.*
import org.kalasim.misc.repeat
import java.lang.IllegalArgumentException
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

    fun invert(): Direction = when(this){
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
            uniform(0.5, 1.5)()
            hold(uniform(0.5, 1.5).sample() * iat)
        }
    }
}


class Visitor(from: Int, to: Int) : Component() {

    val building = get<Building>()

    val fromFloor = building.floors[from]
    val toFloor = building.floors[to]
    val direction = if(from < to) UP else DOWN

    override fun process() = sequence {
        fromFloor.add(this@Visitor)

        // call car if not yet done by another visitor waiting
        // on the same floor for the same direction
        get<Requests>().putIfAbsent(fromFloor to  direction, env.now)

        building.cabs.firstOrNull { isPassive }?.activate()

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
                openDoor()
                leaving.forEach {
                    it.leave(visitors)
                    activate()
                }

                hold(EXIT_TIME, description = "Passengers exiting")
            }

//            if(direction == STILL) direction  = listOf(UP, DOWN).random() // todo not deterministic
            if(direction == STILL) direction  = enumerated(UP, DOWN).sample()

            val requests = get<Requests>()

            // try to continue in the same direction or change
            for(dir in listOf(direction, direction.invert())){
                if(requests.containsKey(floor to dir)){
                    requests.remove(floor to dir)

                    openDoor()

                    val zusteiger = floor.components
                        .filter { it.direction == dir }
                        .take(capacity - visitors.size)

                    zusteiger.forEach { it.leave(floor)}
                    zusteiger.forEach { visitors.add(it)}
                    hold(ENTER_TIME, description = "Letting new passengers in")

                    val countInDirection = visitors.components.count{it.direction == dir}
                    if(countInDirection > 0){
                        requests.putIfAbsent(floor to  direction, env.now)
                    }
                }
            }

            closeDoor()

            val floors = get<Building>().floors

            if(direction != STILL) {
                val nextFloor = floors[floors.indexOf(floor) + direction.asIncrement()]
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

class Building : Component() {
    val floors = repeat(1+ TOP_FLOOR) { Floor() }
    val cabs = repeat(NUM_CARS) { Car(floors.first()) }
}


typealias Floor = ComponentQueue<Visitor>
typealias Requests = MutableMap<Pair<Floor, Direction>, Double>

fun main() {

    createSimulation(true) {
        dependency { Building() }

        val requests: Requests = mutableMapOf()
        dependency { requests }

        VisitorGenerator(0 to 0, 1 to TOP_FLOOR, LOAD_0_n, "vg_0_n")
        VisitorGenerator(1 to TOP_FLOOR, 0 to 0, LOAD_n_0, "vg_n_0")
        VisitorGenerator(1 to TOP_FLOOR, 1 to TOP_FLOOR, LOAD_n_n, "vg_n_nn")

        run(1000)
    }
}