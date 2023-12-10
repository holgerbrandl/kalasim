package org.kalasim.logistics

import org.kalasim.animation.*
import java.lang.Double.max

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



