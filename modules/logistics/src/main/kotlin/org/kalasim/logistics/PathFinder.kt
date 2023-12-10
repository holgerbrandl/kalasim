package org.kalasim.logistics

import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.kalasim.animation.Point
import org.kalasim.animation.kmh
import org.kalasim.logistics.MovementDirection.Forward
import org.kalasim.logistics.MovementDirection.Reverse

enum class MovementDirection { Forward, Reverse }

data class DirectedPathSegment(val segment: PathSegment, val direction: MovementDirection) {
    val endPoint: Point
        get() = when(direction) {
            Forward -> Point(segment.to.position.x, segment.to.position.y)
            Reverse -> Point(segment.from.position.x, segment.from.position.y)
        }

    override fun toString(): String = segment.id + if(direction == Forward) "(->)" else "(<-)"
}

/**
 * Class representing a path finder in a given road network.
 *
 * @param network The network represented as a `GeoMap`.
 */
class PathFinder(val network: GeoMap) {

    // todo We may want to have on graph with different edge weight (e.g. with differnt max speed of the cars)
    // If your graph implements WeightedGraph, you can use the setEdgeWeight method to adjust weights dynamically.

    val pathGraph: Graph<Node, DirectedPathSegment> =
        DefaultDirectedWeightedGraph(DirectedPathSegment::class.java)

    private val pathCache: MutableMap<Pair<Port, Port>, GeoPath> = mutableMapOf()

    init {
        network.nodes.forEach {
            pathGraph.addVertex(it)
        }

        network.segments.forEach { segment ->
            with(segment) {
                pathGraph.addEdge(from, segment.to, DirectedPathSegment(segment, Forward))

                //weight of the edge (we default to a very fast max speed if not defined)
                val calculatedWeight = (length / (speedLimit ?: 100.kmh)).inWholeSeconds.toDouble()

                pathGraph.setEdgeWeight(from, to, calculatedWeight)

                // consider directionality
                if(segment.bidirectional) {
                    pathGraph.addEdge(to, from, DirectedPathSegment(segment, Reverse))
                    pathGraph.setEdgeWeight(to, from, calculatedWeight)
                }
            }
        }
    }


    fun findPath(from: Port, to: Port): GeoPath = pathCache.getOrPut(Pair(from, to)) {
        val sourceNode = from.segment.to
        val targetNode = to.segment.from

        val dijkstraAlg = DijkstraShortestPath(pathGraph)
        val iPaths = dijkstraAlg.getPaths(sourceNode)

        val path: GraphPath<Node, DirectedPathSegment> = iPaths.getPath(targetNode)

        val segments = path.vertexList.zipWithNext().map { (from, to) ->
            val segForward = network.segments.firstOrNull { it.from == from && it.to == to }
            val segReverse = network.segments.firstOrNull { it.from == to && it.to == from }

            segForward ?: segReverse
        }

        GeoPath(from, to, path)
    }

}

data class GeoPath(val from: Port, val to: Port, val route: GraphPath<Node, DirectedPathSegment>)
