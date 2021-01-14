package org.kalasim.examples.elevator

import org.kalasim.*
import org.kalasim.examples.elevator.Car.DoorState.*
import org.kalasim.examples.elevator.Direction.*
import org.kalasim.misc.repeat

// Adopted from https://github.com/salabim/salabim/blob/master/sample%20models/Elevator.py


const val up = 1
const val still = 0
const val down = -1

const val MOVE_TIME = 10
const val DOOR_OPEN_TIME = 3
const val DOOR_CLOSE_TIME = 3
const val enter_time = 3
const val EXIT_TIME = 3

const val LOAD_0_n = 50
const val LOAD_n_n = 100
const val LOAD_n_0 = 100
const val CAR_CAPACITY = 4
const val NUM_CARS = 3
const val groundFloor = 0
const val TOP_FLOOR = 15

enum class Direction { DOWN, STILL, UP;

    fun asIncrement(): Int = when(this){
        DOWN -> -1
        STILL -> 0
        UP -> 1
    }
}

//@Suppress("EXPERIMENTAL_FEATURE_WARNING")
//inline class Floor(val value:Int)


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
    val direction = if(from < to) UP else DOWN

    val building = get<Building>()

    val fromFloor = building.floors[from]
    val toFloor = building.floors[to]


    override fun process() = sequence<Component> {
        fromFloor.add(this@Visitor)

        building.cabs.firstOrNull { isPassive }?.activate()
    }
}


class Car(val capacity: Int = CAR_CAPACITY) : Component() {
    var direction = STILL

    val floors = get<Building>().floors
    var floor = floors.first()

    // todo why is this is queue (it does no seem to be used as such; stats?)
    val visitors = ComponentQueue<Visitor>("visitors in $name")

    enum class DoorState { OPEN, CLOSED }

    var door = CLOSED



    override fun process() = sequence<Component> {
        while(true) {
            if(direction == STILL) passivate()

            val leaving = visitors.components.filter{ it.toFloor ==floor}

            if(leaving.isNotEmpty()){
                openDoor()
                leaving.forEach {
                    it.leave(visitors)
                    activate()
                }

                // let them exit
                log("Allow passengers to exit")
                hold(EXIT_TIME)



                closeDoor()

                if(direction != STILL){
                    val nextFloor = floors[floors.indexOf(floor) + direction.asIncrement()]
                    hold(MOVE_TIME)
                }

            }
        }
    }

    suspend fun SequenceScope<Component>.openDoor() {
        if(door == OPEN) return
        hold(DOOR_OPEN_TIME)
        door = OPEN
    }

    suspend fun SequenceScope<Component>.closeDoor() {
        if(door == OPEN) return
        hold(DOOR_OPEN_TIME)
        door = OPEN
    }


}


class Building : Component() {
    val cabs = repeat(NUM_CARS) { Car() }
    val floors = repeat(TOP_FLOOR) { ComponentQueue<Visitor>() }
}

fun ComponentQueue<Visitor>.countDirection(direction: Direction) {
    q.filter { it.component.direction == direction }.count()
}


fun main() {

    createSimulation(true) {
        dependency { Building() }


        VisitorGenerator(0 to 0, 1 to TOP_FLOOR, LOAD_0_n, "vg_0_n")
        VisitorGenerator(1 to TOP_FLOOR, 0 to 0, LOAD_n_0, "vg_n_0")
        VisitorGenerator(1 to TOP_FLOOR, 1 to TOP_FLOOR, LOAD_n_n, "vg_n_nn")

        run(100)
    }
}