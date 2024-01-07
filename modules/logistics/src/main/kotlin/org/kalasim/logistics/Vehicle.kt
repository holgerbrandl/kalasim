package org.kalasim.logistics

import org.kalasim.*
import org.kalasim.animation.*
import org.kalasim.logistics.MovingState.Stopped
import org.kalasim.misc.InternalKalasimApi


enum class MovingState { Moving, Stopped, Accelerating }
enum class LogicalMovingState { EnteringSegment, LeavingSegment }


open class Vehicle(
    val startingPosition: RelativeSegmentPosition,
    name: String? = null,
    val maxSpeed: Speed = 100.kmh
//    val acc: Acceleration = 2.acc
) : AnimationComponent(startingPosition.position, name) {

    constructor(
        startingPosition: Pair<Port, MovementDirection>,
        name: String? = null,
        maxSpeed: Speed = 100.kmh
    ) : this(startingPosition.first.toRelSegmentPosition(startingPosition.second), name, maxSpeed) {
        currentPort = startingPosition.component1()
    }

    constructor(
        startingPosition: Port,
        name: String? = null,
        maxSpeed: Speed = 100.kmh
    ) : this(startingPosition to MovementDirection.Forward, name, maxSpeed)

    var followVehicle: Vehicle? = null

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port? = null
    var currentSegment: DirectedPathSegment = startingPosition.directedPathSegment

    val movingState = State(Stopped)

    @Deprecated("not needed")
    val logicalMovementState = State(LogicalMovingState.EnteringSegment)

    val occupancyTracker = get<PathOccupancyTracker>().apply {
        enteringSegment(this@Vehicle, currentSegment)
    }

    val pathFinder by lazy { get<PathFinder>() }


    fun moveTo(target: Port) = sequence {
        moveTo(target)
    }


    suspend fun SequenceScope<Component>.moveTo(target: Port) {
        logger.info { "computing path from $startingPosition from to $target" }

        if(target == currentPort) {
            logger.info { "skipping movement because target '$target' matches current position" }
            return
        }

        val path = pathFinder.findPath(currentPort!!, target)

//        if(path.route.isEmpty()){
        require(path.route.isNotEmpty()) {
            logger.error { "Could not find route from $startingPosition to $target" }
        }

        currentPort = null

        moveAlongPath(path.route.map { it.end })

        logger.info { "reached target $target at $startingPosition" }

        currentPort = target
    }


    suspend fun SequenceScope<Component>.moveAlongPath(wayPoints: List<RelativeSegmentPosition>) {
//        wayPoints.map{ it.end}.forEach { wayPoint->
        wayPoints.forEach { wayPoint ->
            //            logicalMovementState.value = LogicalMovingState.EnteringSegment

            moveToCO(wayPoint)
            //            logicalMovementState.value = LogicalMovingState.LeavingSegment
        }
    }

    fun enterNetwork(port: Port) {

    }

    fun exitNetwork(port: Port) {

    }


    suspend fun SequenceScope<Component>.moveToCO(nextTarget: RelativeSegmentPosition) {
        occupancyTracker.enteringSegment(this@Vehicle, nextTarget.directedPathSegment)
        currentSegment = nextTarget.directedPathSegment


        val directedSegment = nextTarget.directedPathSegment
        val vehicleDistEndPoint = this@Vehicle.currentPosition.distance(nextTarget.position)

        val collisionCandidate = occupancyTracker.findVehicles(directedSegment).filter {
            it != this@Vehicle
        }.filter {
            // remove vehicles behind
            //TODO target might be wrong reference --> use relative ref instead
            vehicleDistEndPoint > it.currentPosition.distance(nextTarget.position)
        }.maxByOrNull {
            // find closest
            it.currentPosition.distance(nextTarget.position)
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

        // TODO followVehicle not set --> what if it stops
        if(predictedCollision != null && predictedCollision.second.relativePosition < 1.0) {
            val (timeUntilCol, relColCoord) = predictedCollision

            logger.info { "predicted moving to collision point $predictedCollision" }

            // what if it stops again?
//            move(nextTarget.absPosition, speed, "moving ${this@Vehicle} to $nextTarget")

            // not null, that we collide
            val beforeCollision = relColCoord - 5.meters //todo bring back
//            movingState.value = Accelerating
//            movingState.value = MovingState.Moving
            move(beforeCollision, maxSpeed, "moving ${this@Vehicle} to $nextTarget")

            // spawn sub-processes to keep distance and to continue
//            val distanceChecker = DistanceChecker(collisionCandidate, this@Vehicle)
            collisionCandidate.followVehicle = this@Vehicle

            //todo needed? --> yes if vehicle disappears at port
//            val segChangeListener = SegmentChangeTracker(collisionCandidate, this@Vehicle)

            move(
                nextTarget,
                speed = collisionCandidate.currentSpeed,
                description = "moving ${this@Vehicle} to $nextTarget"
            )

//            distanceChecker.cancel()
            followVehicle = null
//            segChangeListener.cancel()
        } else {
            logger.info { "no collision candidate detected down the patth, proceeding until final target '$nextTarget' on segment '$currentSegment'" }

            move(nextTarget, maxSpeed, "moving ${this@Vehicle} to $nextTarget")
        }
    }

    suspend fun SequenceScope<Component>.move(
        nextTarget: RelativeSegmentPosition,
        speed: Speed,
        description: String? = null,
        priority: Priority = Priority.NORMAL,
    ) {
        followVehicle?.adjustSpeed(speed)
        move(nextTarget.position, speed, description, priority)
    }

    @OptIn(InternalKalasimApi::class)
    fun adjustSpeed(predecSpeed: Speed? = null) {
        val newSpeed = predecSpeed ?: this.maxSpeed

        if(currentSpeed == newSpeed) {
            return
        }

        // update current status and reschedule current move with new speed
        val distance = to!! - currentPosition

        val duration = distance / newSpeed
        estimatedArrival = now + duration
        started = now

        currentSpeed = newSpeed
        // inherit priority from parent

        env.remove(this)
        reschedule(estimatedArrival, description = "moving to $to", type = ScheduledType.HOLD)
        //duration, description ?: "moving to $nextTarget", priority = priority)

        // inform follow vehicle
        followVehicle?.adjustSpeed(newSpeed)
    }
}

//class DistanceChecker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
//    override fun repeatedProcess() = sequence {
//        logger.info { "monitoring for preceding vehicle '$collisionCandidate' for speed change " }
//        wait(collisionCandidate.movingState turns Accelerating)
//
//        logger.info { "detected speed-chang at preceding vehicle '$collisionCandidate'" }
//        vehicle.adjustSpeed(collisionCandidate.currentSpeed)
//    }
//}

class SegmentChangeTracker(val collisionCandidate: Vehicle, val vehicle: Vehicle) : Component() {
    override fun repeatedProcess() = sequence {
        wait(collisionCandidate.logicalMovementState turns LogicalMovingState.LeavingSegment)
        vehicle.adjustSpeed()
    }
}
