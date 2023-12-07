package org.kalasim.logistics

import kotlin.math.pow
import kotlin.math.sqrt

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
        get() = sqrt((from.position.x - to.position.x).pow(2.0) + (from.position.y - to.position.y).pow(2.0))
}

open class Port(val id: String, val distance: Double, val segment: PathSegment) {
    val position: Point = Point(
        segment.from.position.x + distance * (segment.to.position.x - segment.from.position.x),
        segment.from.position.y + distance * (segment.to.position.y - segment.from.position.y)
    )

    override fun toString() = id
}

data class GeoMap(val segments: List<PathSegment>, val nodes: Collection<Node>, val ports: List<Port> = listOf()) {

    fun getLimits(expand: Double = 0.2): Rectangle {
        val minX = nodes.minOf { it.position.x }
        val minY = nodes.minOf { it.position.y }
        val maxX = nodes.maxOf { it.position.x }
        val maxY = nodes.maxOf { it.position.y }

        val xRange = maxX - minX
        val yRange = maxY - minY

        val upperLeft = Point(minX - xRange * expand, minY - yRange * expand)
        val lowerRight = Point(maxX + xRange * expand, maxY + yRange * expand)

        return Rectangle(upperLeft, lowerRight)
    }
}

data class StartingPosition(val port: Port)




