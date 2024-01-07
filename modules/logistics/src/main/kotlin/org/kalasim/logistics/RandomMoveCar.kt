package org.kalasim.logistics

import org.kalasim.animation.Speed
import org.kalasim.animation.kmh
import kotlin.time.Duration.Companion.seconds

class RandomMoveCar(startingPosition: Building, speed: Speed = 100.kmh) :
    Vehicle(startingPosition.port, maxSpeed = speed) {

    var lastPosition = startingPosition

    override fun repeatedProcess() = sequence {
        val destination = get<CityMap>().buildings.random(random)
        // option1: toggle subprocess; Note: car won't repeat when doing so
//            activate(process = ::moveTo, processArgument = destination)

        logger.info { "Moving from $lastPosition to $destination" }
        // option2: yield to keep repeated-process going
        moveTo(destination.port)
        logger.info { "Arrived at $lastPosition" }

        hold(15.seconds)
    }
}