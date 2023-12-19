package org.kalasim.logistics

import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultDirectedWeightedGraph
import org.kalasim.animation.distanceTo
import org.kalasim.animation.kmh
import org.kalasim.logistics.MovementDirection.Forward
import org.kalasim.logistics.MovementDirection.Reverse
import kotlin.time.DurationUnit


/**
 * Class representing a path finder in a given road network.
 *
 * @param geoMap The network represented as a `GeoMap`.
 */
class PathFinder(val geoMap: GeoMap) {

    // todo We may want to have on graph with different edge weight (e.g. with different max speed of the cars)
    // If your graph implements WeightedGraph, you can use the setEdgeWeight method to adjust weights dynamically.

    val pathGraph: Graph<MapPosition, RelativeSegmentEdge> =
        DefaultDirectedWeightedGraph(RelativeSegmentEdge::class.java)

    private val pathCache: MutableMap<Pair<Port, Port>, GeoPath> = mutableMapOf()

    init {
        geoMap.ports.forEach {
            pathGraph.addVertex(it)
        }

        geoMap.crossings.forEach {
            pathGraph.addVertex(it)
        }

        fun addWeightedEdge(
            from: MapPosition,
            to: MapPosition,
            segment: PathSegment,
            fromPosition: RelativeSegmentPosition,
            toPosition: RelativeSegmentPosition
        ) {
            val calculatedWeight =
                (from.position.distanceTo(to.position) / (segment.speedLimit ?: 500.kmh)).toDouble(DurationUnit.SECONDS)

            require(pathGraph.addEdge(from, to, RelativeSegmentEdge(fromPosition, toPosition))) {
                "duplicated edge addition"
            }
            pathGraph.setEdgeWeight(from, to, calculatedWeight)
        }

        fun addSegment(from: Node, to: Node, segment: PathSegment, direction: MovementDirection) {

            val directedSegment = DirectedPathSegment(segment, direction)

            val fromPosition = RelativeSegmentPosition(directedSegment, 0.0)
            val toPosition = RelativeSegmentPosition(directedSegment, 1.0)

            addWeightedEdge(from, to, segment, fromPosition, toPosition)
        }


        // connect segment end-points in graph
        geoMap.segments.forEach { segment ->
            with(segment) {
                addSegment(from, to, segment, Forward)

                if(segment.bidirectional) {
                    addSegment(to, from, segment, Reverse)
                }
            }
        }

        fun computeRelSegmentPosition(
            segment: PathSegment,
            direction: MovementDirection,
            isStart: Boolean
        ): RelativeSegmentPosition {
            val directedSegment = DirectedPathSegment(segment, direction)

            return RelativeSegmentPosition(directedSegment, if(isStart) 0.0 else 1.0)
        }

        //  port connections  to segment end-points
        geoMap.ports.forEach {
            if(it.directionality.isForward) {
                // outgoing
                addWeightedEdge(
                    it,
                    it.segment.to,
                    it.segment,
                    it.toRelSegmentPosition(Forward),
                    computeRelSegmentPosition(it.segment, Forward, false)
                )

                // incoming
                addWeightedEdge(
                    it.segment.from,
                    it,
                    it.segment,
                    computeRelSegmentPosition(it.segment, Forward, true),
                    it.toRelSegmentPosition(Forward),
                )
            }
            if(it.directionality.isReverse) {
                // outgoing
                addWeightedEdge(
                    it,
                    it.segment.to,
                    it.segment,
                    it.toRelSegmentPosition(Forward),
                    computeRelSegmentPosition(it.segment, Reverse, false)
                )

                // incoming
                addWeightedEdge(
                    it.segment.from,
                    it,
                    it.segment,
                    computeRelSegmentPosition(it.segment, Reverse, true),
                    it.toRelSegmentPosition(Reverse)
                )
            }
        }

        // We should also connect ports on the same segment to allow for short distance travel
        geoMap.ports.groupBy { it.segment }.forEach { (segment, ports) ->
            ports.forEach { port ->
                if(port.directionality.isForward) {
                    ports.filter { other -> other.distance >= port.distance && other != port }
                        .forEach { otherPort ->
                            addWeightedEdge(
                                port,
                                otherPort,
                                segment,
                                port.toRelSegmentPosition(Forward),
                                otherPort.toRelSegmentPosition(Forward)
                            )
                        }
                }

                if(port.directionality.isReverse) {
                    ports.filter { other -> other.distance < port.distance }
                        .forEach { otherPort ->
                            addWeightedEdge(
                                port,
                                otherPort,
                                segment,
                                port.toRelSegmentPosition(Reverse),
                                otherPort.toRelSegmentPosition(Reverse)
                            )
                        }
                }
            }
        }
    }

    fun findPath(from: Port, to: Port): GeoPath = pathCache.getOrPut(Pair(from, to)) {
//        val sourceNode = from.segment.to
//        val targetNode = to.segment.from
        val sourceNode = from
        val targetNode = to

        val dijkstraAlg = DijkstraShortestPath(pathGraph)
        val iPaths = dijkstraAlg.getPaths(sourceNode)

//        val path = iPaths.getPath(targetNode)
//
//        val segments = path.vertexList.zipWithNext().map { (from, to) ->
//            val segForward = geoMap.segments.firstOrNull { it.from == from && it.to == to }
//            val segReverse = geoMap.segments.firstOrNull { it.from == to && it.to == from }
//
//            segForward ?: segReverse
//        }

        GeoPath(from, to, iPaths.getPath(targetNode).edgeList.toList())
    }
}


// todo what the benefit of the wrapper? cant it be simplified?
data class GeoPath(
    val from: Port,
    val to: Port,
    val route: List<RelativeSegmentEdge>
) {


//    fun toSegments() = route.edgeList
//    fun toSegments(): List<RelativeSegmentPosition> = buildList {
//        if(route.edgeList.isEmpty()) {
//            if(from.toDirectedPathSegment() != to.toDirectedPathSegment()) {
//                add(RelativeSegmentPosition(from.toDirectedPathSegment(), 1.0))
//            } else {
//                require(from.toRelSegmentPosition(startingPosition.second).relativePosition > to.toRelSegmentPosition(
//                    startingPosition.second
//                ).relativePosition)
//            }
//        } else {
//            add(RelativeSegmentPosition(from.toDirectedPathSegment(), 1.0))
//            route.edgeList.forEach { add(RelativeSegmentPosition(it, 1.0)) }
//        }
//
//        add(to.toRelSegmentPosition(startingPosition.second))
//    }
}
