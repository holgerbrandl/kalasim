package org.kalasim.logistics

import org.jgrapht.Graph
import org.jgrapht.GraphPath
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge

/**
 * Class representing a path finder in a given road network.
 *
 * @param network The network represented as a `GeoMap`.
 */
class PathFinder(network: GeoMap) {

    // todo We may want to have on graph with different edge weight (e.g. with differnt max speed of the cars)
    // If your graph implements WeightedGraph, you can use the setEdgeWeight method to adjust weights dynamically.

    val pathGraph: Graph<Node, DefaultWeightedEdge> =
        DefaultDirectedWeightedGraph(DefaultWeightedEdge::class.java)

    private val pathCache: MutableMap<Pair<Port, Port>, GeoPath> = mutableMapOf()

    init {
        network.nodes.forEach {
            pathGraph.addVertex(it)
        }

        network.segments.forEach { segment ->
            with(segment) {
                pathGraph.addEdge(from, segment.to)

                //weight of the edge (we default to a very fast max speed if not defined)
                val calculatedWeight = length / (speedLimit?.meterPerSecond ?: 100.0)

                pathGraph.setEdgeWeight(from, to, calculatedWeight)

                // consider directionality
                if(segment.bidirectional) {
                    pathGraph.addEdge(to, from)
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

        val path: GraphPath<Node, DefaultWeightedEdge> = iPaths.getPath(targetNode)

        GeoPath(from, to, path)
    }

}

data class GeoPath(val from: Port, val to: Port, val route: GraphPath<Node, DefaultWeightedEdge>)
