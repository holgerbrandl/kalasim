package org.kalasim.logistics

import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


class PathOccupancyTracker {
    fun enteringSegment(vehicle: Vehicle, directedSegment: DirectedPathSegment) {
        roadOccupancy.toList().firstOrNull { it.second.contains(vehicle) }?.second?.remove(vehicle)

        roadOccupancy.getOrPut(directedSegment) { mutableListOf() }.add(vehicle)
    }

    val roadOccupancy = mutableMapOf<DirectedPathSegment, MutableList<Vehicle>>()

}


class Crossing(numVehicles: Int = 2, xBlocks: Int = 2, yBlocks: Int = 2, numBuildings: Int = 10) : Environment() {

    val cityMap = dependency { buildCity(xBlocks, yBlocks, numBuildings = numBuildings) }
//    val cityMap = dependency { buildCity(15, 15, numBuildings = 100) }

    val geomMap = dependency { cityMap.toGeoMap() }
    val roadManager = dependency { PathOccupancyTracker() }

    init {
        dependency { PathFinder(geomMap) }
    }


    class Car(startingPosition: Building, speed: Speed = 100.kmh) : Vehicle(startingPosition.port, speed) {

        var lastPosition = startingPosition

        override fun repeatedProcess() = sequence {
            hold(2.minutes)

            val destination = get<CityMap>().buildings.random(random)
            // option1: toggle subprocess; Note: car won't repeat when doing so
//            activate(process = ::moveTo, processArgument = destination)

            logger.info { "Moving from $lastPosition to $destination" }
            // option2: yield to keep repeated-process going
            yieldAll(moveTo(destination.port))
            logger.info { "Arrived at $lastPosition" }
        }
    }


    val cars = List(numVehicles) {
        Car(cityMap.buildings.random(random), random.nextInt(50, 150).kmh)
    }
}

fun main() {
    val sim = Crossing(2).apply {
        enableComponentLogger()
    }

    sim.run(100.days)
}
