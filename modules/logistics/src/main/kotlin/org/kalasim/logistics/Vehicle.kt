package org.kalasim.logistics

import org.kalasim.*
import org.kalasim.animation.*
import org.kalasim.logistics.MovingState.Accelerating
import org.kalasim.logistics.MovingState.Stopped


enum class MovingState { Moving, Stopped, Accelerating }
enum class LogicalMovingState { EnteringSegment, LeavingSegment }

open class Vehicle(startingPosition: Port, val speed: Speed = 100.kmh, val acc: Acceleration = 2.acc) :
    AnimationComponent(startingPosition.position) {

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port? = startingPosition


    val movingState = State(Stopped)
    val logicalMovementState = State(LogicalMovingState.EnteringSegment)

    val occupancyTracker = get<PathOccupancyTracker>()

    init {
        val segmentOccupancy: DirectedPathSegment = when(startingPosition.directionality) {
            PortConnectivity.Forward -> DirectedPathSegment(startingPosition.segment, MovementDirection.Forward)
            PortConnectivity.Reverse -> DirectedPathSegment(startingPosition.segment, MovementDirection.Reverse)
            PortConnectivity.Bidirectional -> DirectedPathSegment(startingPosition.segment, MovementDirection.Forward)
        }

        occupancyTracker.enteringSegment(this@Vehicle, segmentOccupancy)
    }

    val pathFinder = get<PathFinder>()

    fun moveTo(target: Port): Sequence<Component> {
        val occupancyTracker = get<PathOccupancyTracker>()

        return sequence {
            //        logger.info{ "computing port from $currentPort to $target"}
            val path = pathFinder.findPath(currentPort!!, target)

            logger.info { "computed port from $ to $target" }

            val startPort = currentPort
            currentPort = null
            move(startPort!!.segment.to.position, speed = speed, description = "moving ${this@Vehicle}")

            path.route.edgeList.forEach { directedSegment ->
                occupancyTracker.enteringSegment(this@Vehicle, directedSegment)

                val nextTarget = directedSegment.endPoint

                val vehicleDistEndPoint = this@Vehicle.currentPosition.distance(directedSegment.endPoint)

                val collisionCandidate = occupancyTracker.findVehicles(directedSegment).filter {
                    it != this@Vehicle
                }.filter {
                    // remove vehicles behind
                    vehicleDistEndPoint > it.currentPosition.distance(directedSegment.endPoint)
                }.maxByOrNull {
                    // find closest
                    it.currentPosition.distance(directedSegment.endPoint)
                }

                // ranfahren & match speed (evtl port direkt anfahren und gegner ausser acht lassen, falls bis ankunft vorbeigefahren)

                // compute collision on current path segment
                val predictedCollision: Point? = collisionCandidate?.let { computeCollision(this@Vehicle, it) }

                // predictedCollision --> null: no collison on path segment

                if(predictedCollision != null) {
                    // what if it stops again?
                    move(nextTarget, speed, "moving ${this@Vehicle} to $nextTarget")


                    // not null, that we collide
                    val beforeCollision = predictedCollision //- 5.meters todo bring back
                    move(beforeCollision, speed, "moving ${this@Vehicle} to $nextTarget")

                    // match speed
//                if(collisionCandidate.movingState.value == Stopped){
//                    // stau
//                    wait(collisionCandidate.movingState turn moving)
//                }
                    val distanceChecker = DistanceChecker(collisionCandidate, this@Vehicle)
                    val segChangeListener = SegmentChangeTracker(collisionCandidate, this@Vehicle)

                    move(
                        nextTarget,
                        speed = collisionCandidate.speed,
                        description = "moving ${this@Vehicle} to $nextTarget"
                    )

                    move(nextTarget, speed, "moving ${this@Vehicle} to $nextTarget")

                    distanceChecker.cancel()
                    segChangeListener.cancel()
                } else {
                    move(nextTarget, speed, "moving ${this@Vehicle} to $nextTarget")
                }
            }

            //todo extract CO as function and apply here as well
            move(target.position, speed = speed, description = "moving ${this@Vehicle}")

            logger.info { "reached target from $ to $target" }

            currentPort = target
        }
    }

    private fun computeCollision(vehicle: Vehicle, collisionCandidate: Vehicle): Point? {
        return null
    }

    fun adjustSpeed(maxSpeed: Speed? = null) {
        //todo do not interrupt or cancel running component process
        val newSpeed = maxSpeed ?: speed

        // todo cap on max segement speed

        // next
        // wo stehts
        // adjust currentSpeed and reschedule hold
//        reschedule()

    }
}

class DistanceChecker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
    override fun repeatedProcess() = sequence<Component> {
        wait(collisionCandidate.movingState turns Accelerating)
        vehicle.adjustSpeed(collisionCandidate.currentSpeed)
    }
}

class SegmentChangeTracker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
    override fun repeatedProcess() = sequence<Component> {
        wait(collisionCandidate.logicalMovementState turns LogicalMovingState.LeavingSegment)
        vehicle.adjustSpeed()
    }
}
