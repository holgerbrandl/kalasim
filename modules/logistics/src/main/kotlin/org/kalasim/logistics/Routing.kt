package org.kalasim.logistics

import org.kalasim.animation.*
import java.lang.Double.max
import kotlin.time.Duration

// define routing model

open class Node(val id: String, final override val position: Point) : MapPosition {
    val x = position.x
    val y = position.y

    fun euclideanDistanceTo(other: Node) = (position - other.position).absoluteValue

    override fun toString() = id
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

    override fun toString() = "$id($from->$to)"
}


fun PathSegment.containsPoint(point: Point, epsilon: Distance = 0.1.meters): Boolean {
    val start = from.position
    val end = to.position

    // Calculate the distance from the point to both ends of the segment
    val distanceFromStart = point.distanceTo(start)
    val distanceFromEnd = point.distanceTo(end)

    // Calculate the total length of the segment
    val segmentLength = from.euclideanDistanceTo(to)

    // Check if the sum of distances is approximately equal to the length of the segment
//    return (distanceFromStart + distanceFromEnd - segmentLength).absoluteValue- < epsilon
    return false
}

// Function to calculate Euclidean distance between two points
//fun euclideanDistance(p1: Point, p2: Point): Double {
//    return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
//}



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

    fun relativePosition(currentPosition: Point): Double {
        val prop = (currentPosition - start) / segment.length
        return when(direction) {
            MovementDirection.Forward -> prop
            MovementDirection.Reverse -> 1 - prop
        }
    }

    fun relativeSegmentPosition(currentPosition: Point) =
        RelativeSegmentPosition(this, relativePosition(currentPosition))

    override fun toString(): String =
        segment.toString() + (if(direction == MovementDirection.Forward) "[>>]" else "[<<]")
}

interface MapPosition {
    val position: Point

//    val x: Double
//    val y: Double
}

//typealias MapPosition= Point

data class RelativeSegmentPosition(
    val directedPathSegment: DirectedPathSegment,
    val relativePosition: Double
) : MapPosition {

    operator fun plus(distance: Distance): RelativeSegmentPosition {
        val relativeMovement = distance.meters / directedPathSegment.segment.length.meters

        return RelativeSegmentPosition(directedPathSegment, relativePosition + relativeMovement)
    }

    operator fun minus(distance: Distance) = plus(-distance)

    // just needed for internal validation
    fun upstreamOf(end: RelativeSegmentPosition): Boolean = relativePosition < end.relativePosition

    override val position: Point
        get() = Point(
            directedPathSegment.start.x + (directedPathSegment.end.x - directedPathSegment.start.x) * relativePosition,
            directedPathSegment.start.y + (directedPathSegment.end.y - directedPathSegment.start.y) * relativePosition
        )

    override fun toString() =
        "RSP(segment=$directedPathSegment, position=[${position.x}, ${position.y}], rp=$relativePosition)"
}

data class RelativeSegmentEdge(val start: RelativeSegmentPosition, val end: RelativeSegmentPosition) {
    init {
        // validate edge integrity
        require(start.upstreamOf(end)) {
            "invalid order of start $start and end $end on segment"
        }
        require(start.directedPathSegment == end.directedPathSegment)
    }
}


//fun Port.toDirectedPathSegment(): DirectedPathSegment {
//    return when(directionality) {
//        PortConnectivity.Forward -> DirectedPathSegment(segment, MovementDirection.Forward)
//        PortConnectivity.Reverse -> DirectedPathSegment(segment, MovementDirection.Reverse)
//        PortConnectivity.Bidirectional -> DirectedPathSegment(segment, MovementDirection.Forward)
//    }
//}

fun Port.toRelSegmentPosition(direction: MovementDirection): RelativeSegmentPosition = when(direction) {
    MovementDirection.Forward -> {
        RelativeSegmentPosition(DirectedPathSegment(segment, MovementDirection.Forward), distance)
    }

    MovementDirection.Reverse -> {
        require(segment.bidirectional)
        RelativeSegmentPosition(DirectedPathSegment(segment, MovementDirection.Reverse), 1 - distance)
    }
}


enum class PortConnectivity {

    Forward, Reverse, Bidirectional;

    val isForward: Boolean
        get() = (this == Forward) || (this == Bidirectional)

    val isReverse: Boolean
        get() = (this == Reverse) || (this == Bidirectional)
}


open class Port(
    id: String,
    val distance: Double,
    val segment: PathSegment,
    // todo make bidirectional the default as it's more generic
    val directionality: PortConnectivity = PortConnectivity.Forward
) : Node(
    id, Point(
        segment.from.position.x + distance * (segment.to.position.x - segment.from.position.x),
        segment.from.position.y + distance * (segment.to.position.y - segment.from.position.y)
    )
) {
    override fun toString() = id
}


//
//fun Port.toDirectedPathSegment(): DirectedPathSegment {
//    return when(directionality) {
//        PortConnectivity.Forward -> DirectedPathSegment(segment, MovementDirection.Forward)
//        PortConnectivity.Reverse -> DirectedPathSegment(segment, MovementDirection.Reverse)
//        PortConnectivity.Bidirectional -> DirectedPathSegment(segment, MovementDirection.Forward)
//    }
//}

data class GeoMap(
    val segments: List<PathSegment>,
    val ports: List<Port> = listOf()
) {

    init {
        require(ports.distinctBy { it.position }.size == ports.size) {
            "no pair of ports must have the same exact map position"
        }
    }

    val crossings: Set<Node> = (segments.map { it.from } + segments.map { it.to }).toSet()

    fun getLimits(expand: Double = 0.2, minSize: Double = 20.0): Rectangle {
        val minX = crossings.minOf { it.position.x }
        val minY = crossings.minOf { it.position.y }
        val maxX = crossings.maxOf { it.position.x }
        val maxY = crossings.maxOf { it.position.y }

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
    val timeToCollision = (precedingPos.position - ownPosition.position) / (ownSpeed - precedingSpeed)

    // compute collision coordinates
    val colCoord = ownPosition + (ownSpeed * timeToCollision)

    require(timeToCollision > Duration.ZERO) { "negative collision duration has unlikely semantics" }

    return timeToCollision to colCoord
}
