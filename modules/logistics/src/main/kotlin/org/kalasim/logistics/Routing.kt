package org.kalasim.logistics

import org.kalasim.animation.*
import java.lang.Double.max
import kotlin.time.Duration

// define routing model

open class Node(val id: String, val position: Point) {
    val x = position.x
    val y = position.y
}


/**
 * Class representing a path segment between two nodes in a transport network.
 *
 * @property id The unique identifier of the path segment.
 * @property from The node from which the path segment starts.
 * @property to The node to which the path segment leads.
 * @property bidirectional Indicates whether the path segment allows travel in both directions.
 */
open class PathSegment(
    val id: String,
    val from: Node,
    val to: Node,
    val bidirectional: Boolean = true,
    val speedLimit: Speed? = null,
    var capacity: Int = 1
) {

    val length
        get() = from.position - to.position

    override fun toString() = id
}


enum class MovementDirection { Forward, Reverse }

data class DirectedPathSegment(val segment: PathSegment, val direction: MovementDirection) {
    val end: Point
        get() = when(direction) {
            MovementDirection.Forward -> Point(segment.to.position.x, segment.to.position.y)
            MovementDirection.Reverse -> Point(segment.from.position.x, segment.from.position.y)
        }

    val start: Point
        get() = when(direction) {
            MovementDirection.Forward -> Point(segment.from.position.x, segment.from.position.y)
            MovementDirection.Reverse -> Point(segment.to.position.x, segment.to.position.y)
        }

    fun relativePosition(currentPosition: Point): Double = (currentPosition - start) / segment.length
    fun relativeSegmentPosition(currentPosition: Point) =
        RelativeSegmentPosition(this, relativePosition(currentPosition))

    override fun toString(): String = segment.id + if(direction == MovementDirection.Forward) "(->)" else "(<-)"

}

data class RelativeSegmentPosition(val directedPathSegment: DirectedPathSegment, val relativePosition: Double) {
    operator fun plus(distance: Distance): RelativeSegmentPosition {
        val relativeMovement = distance.meters / directedPathSegment.segment.length.meters

        return RelativeSegmentPosition(directedPathSegment, relativePosition + relativeMovement)
    }

    operator fun minus(distance: Distance) = plus(-distance)

    val absPosition: Point
        get() = Point(
            directedPathSegment.start.x + (directedPathSegment.end.x - directedPathSegment.start.x) * relativePosition,
            directedPathSegment.start.y + (directedPathSegment.end.y - directedPathSegment.start.y) * relativePosition
        )
}


fun Port.toDirectedPathSegment(): DirectedPathSegment {
    return when(directionality) {
        PortConnectivity.Forward -> DirectedPathSegment(segment, MovementDirection.Forward)
        PortConnectivity.Reverse -> DirectedPathSegment(segment, MovementDirection.Reverse)
        PortConnectivity.Bidirectional -> DirectedPathSegment(segment, MovementDirection.Forward)
    }
}

fun Port.toRelSegmentPosition(): RelativeSegmentPosition {
    val relativeSegmentPosition: RelativeSegmentPosition = when(directionality) {
        PortConnectivity.Reverse ->
            RelativeSegmentPosition(DirectedPathSegment(segment, MovementDirection.Reverse), 1 - distance)

        PortConnectivity.Forward, PortConnectivity.Bidirectional ->
            RelativeSegmentPosition(DirectedPathSegment(segment, MovementDirection.Forward), distance)
    }
    return relativeSegmentPosition
}


enum class PortConnectivity { Forward, Reverse, Bidirectional }

open class Port(
    val id: String,
    val distance: Double,
    val segment: PathSegment,
    val directionality: PortConnectivity = PortConnectivity.Forward
) {
    val position: Point = Point(
        segment.from.position.x + distance * (segment.to.position.x - segment.from.position.x),
        segment.from.position.y + distance * (segment.to.position.y - segment.from.position.y)
    )

    override fun toString() = id
}

data class GeoMap(val segments: List<PathSegment>, val nodes: Collection<Node>, val ports: List<Port> = listOf()) {

    fun getLimits(expand: Double = 0.2, minSize: Double = 20.0): Rectangle {
        val minX = nodes.minOf { it.position.x }
        val minY = nodes.minOf { it.position.y }
        val maxX = nodes.maxOf { it.position.x }
        val maxY = nodes.maxOf { it.position.y }

        val xRange = maxX - minX
        val yRange = maxY - minY

        val upperLeft = Point(minX - max(xRange * expand, minSize), minY - yRange * expand)
        val lowerRight = Point(maxX + max(xRange * expand, minSize), maxY + yRange * expand)

        return Rectangle(upperLeft, lowerRight)
    }
}


// also see collision_avoidance.md
fun computeCollisionPoint(
    ownPosition: RelativeSegmentPosition,
    precedingPos: RelativeSegmentPosition,
    ownSpeed: Speed,
    precedingSpeed: Speed
): Pair<Duration, RelativeSegmentPosition>? {
    require(ownPosition.directedPathSegment == precedingPos.directedPathSegment)

    if(ownSpeed <= precedingSpeed) return null // collision can't happen if same speed or slower

    // compute time until collision
    val timeToCollision = (precedingPos.absPosition - ownPosition.absPosition) / (ownSpeed - precedingSpeed)

    // compute collision coordinates
    val colCoord = ownPosition + (ownSpeed * timeToCollision)

    require(timeToCollision > Duration.ZERO) { "negative collision duration has unlikely semantics" }

    return timeToCollision to colCoord
}


