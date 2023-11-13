package org.kalasim.logistics

import com.github.holgerbrandl.kdfutils.renameToSnakeCase
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.util.unfold
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import kotlin.math.*
import kotlin.random.Random

fun createPathGrid(
    xBlocks: Int = 16,
    yBlocks: Int = 12,
    blockSize: Double = 10.0,
    seed: Int = 42
): List<PathSegment> {
    val r = Random(seed)

    val blocks: Array<Array<Node?>> = Array(xBlocks) { x ->
        Array(yBlocks) { y ->
            Node("Node_${x}_${y}", Point(x * blockSize, y * blockSize))
        }
    }

    // thin out internals non-uniformly
    repeat((xBlocks*yBlocks*0.1).roundToInt()) {
        val row = blocks[blocks.indices.random(r)]
        row[row.indices.random(r)] = null
    }

    val pathSegments = mutableListOf<PathSegment>()
    fun generateId(from: Node, to: Node): String = "PathSegment_${from.id}_to_${to.id}"

    for((x, row) in blocks.withIndex()) {
        for((y, currentNode) in row.withIndex()) {

            // Skip if this node was thinned out
            if(currentNode == null) continue

            // Validate right neighbor exists and isn't out of grid bounds
            if(x + 1 < blocks.size && blocks[x + 1][y] != null) {
                val neighborNode = blocks[x + 1][y]!!
                val pathSegment = PathSegment(generateId(currentNode, neighborNode), currentNode, neighborNode, true)
                pathSegments.add(pathSegment)
            }

            // Validate lower neighbor exists and isn't out of grid bounds
            if(y + 1 < row.size && blocks[x][y + 1] != null) {
                val neighborNode = blocks[x][y + 1]!!
                val pathSegment = PathSegment(generateId(currentNode, neighborNode), currentNode, neighborNode, true)
                pathSegments.add(pathSegment)
            }
        }
    }

    return pathSegments
}

enum class BuildingType { Business, Factory, Home }
data class Building(val id: String, val area: Rectangle, val port: Port, val type: BuildingType)


fun createBuildings(pathSegments: List<PathSegment>, seed: Int = 42): List<Building> {
    val buildings = mutableListOf<Building>()

    val rectangleDistanceFromSegment = 2.0  // distance of rectangle from pathSegment
    val r = Random(seed)

    val mapCenter = pathSegments.map{it.from.position.x}.average() to pathSegments.map{it.from.position.y}.average()
    val rangeX = pathSegments.map{it.from.position.x}.run { max() - min()  }
    val rangeY = pathSegments.map{it.from.position.y}.run { max() - min()}

    val centerThreshold = rangeX*0.2
    val outsideThreshold = rangeY*0.7

    repeat(pathSegments.size) {
        val segment = pathSegments.random(r)
        val distance =
            sqrt((segment.from.position.x - mapCenter.first).pow(2.0) + (segment.from.position.y - mapCenter.second).pow(2.0)) + r.nextDouble(
                -10.0, 10.00
            ) // Euclidean distance from origin

        val port = Port("port_$it", r.nextDouble(1.0), segment)

        val buildingType = when {
            distance <= centerThreshold -> BuildingType.Business
            distance >= outsideThreshold -> BuildingType.Factory
            else -> BuildingType.Home
        }

        val rectanglePosition = segment.to.position.rotate(segment.from.position, -90.0)
            .normalize()
            .scale(rectangleDistanceFromSegment) // a position that is rectangleDistanceFromSegment away from the path segment
        val building = Building("building_${it}", Rectangle(rectanglePosition, 10.0, 10.0), port, buildingType)

        buildings.add(building)
    }

    return buildings
}

fun buildCity(): Pair<List<PathSegment>, List<Building>> {
    val grid: List<PathSegment> = createPathGrid()
    val buildings: List<Building> = createBuildings(grid)

    grid.toDataFrame()
        .unfold<Node>("from", addPrefix = true)
        .unfold<Node>("to", addPrefix = true)
        .unfold<Point>("from_position", addPrefix = true)
        .unfold<Point>("to_position", addPrefix = true)
        .renameToSnakeCase()
        .writeCSV("segments.csv")

    buildings.toDataFrame()
        .unfold<Port>("port", addPrefix = true)
        .unfold<Point>("port_pathIntersection", addPrefix = true)
        .renameToSnakeCase()
        .writeCSV("buildings.csv")

    // build a city center


    return grid to buildings
}

// highways
fun main() {
    val city = buildCity()

}