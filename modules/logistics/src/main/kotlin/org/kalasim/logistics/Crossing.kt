package org.kalasim.logistics

import org.kalasim.*
import kotlin.time.Duration.Companion.seconds

class Crossing(numVehicles: Int = 1) : Environment() {

    val cityMap = dependency { buildCity(3, 3, numBuildings = 20) }
//    val cityMap = dependency { buildCity(15, 15, numBuildings = 100) }

    val geomMap = dependency { cityMap.toGeoMap() }

    init {
        dependency { PathFinder(geomMap) }
    }


    class Car(startingPosition: Building, speed: Speed = 100.kmh) : Vehicle(startingPosition.port, speed) {

        var lastPosition = startingPosition

        override fun repeatedProcess() = sequence {
            hold(30.seconds)

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
    val sim = Crossing().apply {
        enableComponentLogger()
    }

    sim.run(1.hour)
}