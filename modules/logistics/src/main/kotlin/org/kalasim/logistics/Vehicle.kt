package org.kalasim.logistics

import org.kalasim.animation.AnimationComponent


open class Vehicle(startingPosition: Port, val speed: Speed = 100.kmh, val acc: Acceleration = 2.acc) :
    AnimationComponent(startingPosition.position) {

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port? = startingPosition


    init {
        val segmentOccupancy: DirectedPathSegment = when(startingPosition.directionality) {
            PortConnectivity.Forward -> DirectedPathSegment(startingPosition.segment, MovementDirection.Forward)
            PortConnectivity.Reverse -> DirectedPathSegment(startingPosition.segment, MovementDirection.Reverse)
            PortConnectivity.Bidirectional -> DirectedPathSegment(startingPosition.segment, MovementDirection.Forward)
        }

        get<PathOccupancyTracker>().enteringSegment(this@Vehicle, segmentOccupancy)
    }

    val pathFinder = get<PathFinder>()

    fun moveTo(target: Port) = sequence {
//        logger.info{ "computing port from $currentPort to $target"}
        val path = pathFinder.findPath(currentPort!!, target)

        logger.info { "computed port from $ to $target" }

        val startPort = currentPort
        currentPort = null
        move(startPort!!.segment.to.position, speed = speed.meterPerSecond, description = "moving ${this@Vehicle}")

        path.route.edgeList.forEach { directedSegment ->
            get<PathOccupancyTracker>().enteringSegment(this@Vehicle, directedSegment)

            val nextTarget = directedSegment.targetPosition
            move(nextTarget, speed = speed.meterPerSecond, description = "moving ${this@Vehicle} to $nextTarget")
        }

        move(target.position, speed = speed.meterPerSecond, description = "moving ${this@Vehicle}")

        logger.info { "reached target from $ to $target" }

        currentPort = target
    }
}