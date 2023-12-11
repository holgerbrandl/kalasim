package org.kalasim.logistics

import org.kalasim.*
import org.kalasim.animation.*
import org.kalasim.logistics.MovingState.Accelerating
import org.kalasim.logistics.MovingState.Stopped


enum class MovingState { Moving, Stopped, Accelerating }
enum class LogicalMovingState { EnteringSegment, LeavingSegment }


open class Vehicle(
    startingPosition: Port,
    name: String? = null,
    val maxSpeed: Speed = 100.kmh,
//    val acc: Acceleration = 2.acc
) :
    AnimationComponent(startingPosition.position, name) {

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port? = startingPosition
    var currentSegment: DirectedPathSegment = startingPosition.toDirectedPathSegment()

    val movingState = State(Stopped)
    val logicalMovementState = State(LogicalMovingState.EnteringSegment)

    val occupancyTracker = get<PathOccupancyTracker>().apply {
        enteringSegment(this@Vehicle, currentSegment)
    }

    val pathFinder = get<PathFinder>()


    fun moveTo(target: Port): Sequence<Component> = sequence {
        val path = pathFinder.findPath(currentPort!!, target)

        logger.info { "computed port from $ to $target" }

        currentPort = null

        path.toSegments().forEach { wayPoint ->
            occupancyTracker.enteringSegment(this@Vehicle, wayPoint.directedPathSegment)
            moveToCO(wayPoint)
        }

        logger.info { "reached target from $ to $target" }

        currentPort = target
    }


    private suspend fun SequenceScope<Component>.moveToCO(nextTarget: RelativeSegmentPosition) {
        val directedSegment = nextTarget.directedPathSegment
        val vehicleDistEndPoint = this@Vehicle.currentPosition.distance(nextTarget.absPosition)

        val collisionCandidate = occupancyTracker.findVehicles(directedSegment).filter {
            it != this@Vehicle
        }.filter {
            // remove vehicles behind
            vehicleDistEndPoint > it.currentPosition.distance(nextTarget.absPosition)
        }.maxByOrNull {
            // find closest
            it.currentPosition.distance(nextTarget.absPosition)
        }

        logger.info { "computed preceding vehicle as $collisionCandidate" }

        // ranfahren & match speed (evtl port direkt anfahren und gegner ausser acht lassen, falls bis ankunft vorbeigefahren)

        // compute collision on current path segment
        val predictedCollision = collisionCandidate?.let { precedingVehicle ->
            computeCollisionPoint(
                currentSegment.relativeSegmentPosition(currentPosition),
                currentSegment.relativeSegmentPosition(precedingVehicle.currentPosition),
                maxSpeed,
                precedingVehicle.currentSpeed
            )
        }

        logger.info { "predicted collision point to $predictedCollision" }

        // predictedCollision --> null: no collision on path segment

        if(predictedCollision != null && predictedCollision.second.relativePosition < 1.0) {
            val (timeUntilCol, relColCoord) = predictedCollision

            logger.info { "predicted moving to collision point $predictedCollision" }

            // what if it stops again?
//            move(nextTarget.absPosition, speed, "moving ${this@Vehicle} to $nextTarget")


            // not null, that we collide
            val beforeCollision = relColCoord - 5.meters //todo bring back
            move(beforeCollision.absPosition, maxSpeed, "moving ${this@Vehicle} to $nextTarget")

            // spawn sub-processes to keep distance and to continue
            val distanceChecker = DistanceChecker(collisionCandidate, this@Vehicle)

            //todo needed?
            val segChangeListener = SegmentChangeTracker(collisionCandidate, this@Vehicle)

            move(
                nextTarget.absPosition,
                speed = collisionCandidate.maxSpeed,
                description = "moving ${this@Vehicle} to $nextTarget"
            )

            distanceChecker.cancel()
            segChangeListener.cancel()
        } else {
            logger.info { "no detection, proceeding until final target '$nextTarget' on segment '$currentSegment'" }

            move(nextTarget.absPosition, maxSpeed, "moving ${this@Vehicle} to $nextTarget")
        }
    }


    fun adjustSpeed(maxSpeed: Speed? = null) {
        //todo do not interrupt or cancel running component process
        val newSpeed = maxSpeed ?: this.maxSpeed

        TODO()
        // todo cap on max segement speed

        // next
        // wo stehts
        // adjust currentSpeed and reschedule hold
//        reschedule()

    }
}

class DistanceChecker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
    override fun repeatedProcess() = sequence {
        logger.info { "monitoring for preceding vehicle '$collisionCandidate' for speed change " }
        wait(collisionCandidate.movingState turns Accelerating)

        logger.info { "detected speed-chang at preceding vehicle '$collisionCandidate'" }
        vehicle.adjustSpeed(collisionCandidate.currentSpeed)
    }
}

class SegmentChangeTracker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
    override fun repeatedProcess() = sequence {
        wait(collisionCandidate.logicalMovementState turns LogicalMovingState.LeavingSegment)
        vehicle.adjustSpeed()
    }
}
