package org.kalasim.logistics.examples

import org.kalasim.logistics.*


fun simpleGridMap(): CityMap {
    val roads = createPathGrid(2, 2)

    return CityMap(roads)
}

@Suppress("LocalVariableName")
fun simpleCrossing(): GeoMap {
    val point00 = Point(0.0, 0.0)
    val point01 = Point(0.0, 1.0)
    val point10 = Point(1.0, 0.0)
    val point11 = Point(1.0, 1.0)

    // defining Nodes for each square (each point represents a square in our chessboard)
    val node00 = Node("00", point00)
    val node01 = Node("01", point01)
    val node10 = Node("10", point10)
    val node11 = Node("11", point11)

// now we assume nodes to represent squares on our chessboard, and PathSegments represent the connections between squares

// defining the pathSegments - connections between squares
    val segment00_01 = PathSegment("segment00_01", node00, node01)
    val segment00_10 = PathSegment("segment00_10", node00, node10)
    val segment01_10 = PathSegment("segment01_10", node01, node10)
    val segment01_11 = PathSegment("segment01_11", node01, node11)

    // parking
    val port00_01 = Port("port00_01", 0.2, segment00_01)
    val port00_10 = Port("port00_10", 0.2, segment00_10)
    val port01_10 = Port("port01_10", 0.2, segment01_10)
    val port01_11 = Port("port01_11", 0.2, segment01_11)


    return GeoMap(
        listOf(segment00_01, segment00_10, segment01_10, segment01_11),
        listOf(node00, node01, node11, node10),
        listOf(port00_01, port00_10, port01_10, port01_11)
    )
}