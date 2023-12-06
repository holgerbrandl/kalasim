package org.kalasim.logistics

import com.github.holgerbrandl.kdfutils.renameToSnakeCase
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.util.unfold
import org.jetbrains.kotlinx.dataframe.api.util.unfoldByProperty
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.misc.withExtension
import java.nio.file.Path
import kotlin.math.*
import kotlin.random.Random

fun createPathGrid(
    xBlocks: Int = 16,
    yBlocks: Int = 12,
    blockSize: Double = 50.0,
    seed: Int = 42,
    dropProportion: Double = 0.1
): List<PathSegment> {
    val r = Random(seed)

    val blocks: Array<Array<Node?>> = Array(xBlocks) { x ->
        Array(yBlocks) { y ->
            Node("Node_${x}_${y}", Point(x * blockSize, y * blockSize))
        }
    }

    // thin out internals non-uniformly
    repeat((xBlocks * yBlocks * dropProportion).roundToInt()) {
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

fun trimValue(value: Double, tolerance: Double): Double = when {
    value < tolerance -> tolerance
    value > 1 - tolerance -> 1 - tolerance
    else -> value
}


fun createRectangle(
    port: Port,
    distance: Double,
    width: Double,
    height: Double,
    isLeft: Boolean,
): Rectangle {

    val dx = port.segment.to.position.x - port.segment.from.position.x
    val dy = port.segment.to.position.y - port.segment.from.position.y
    val angle = atan2(dy, dx)

    val halfWidth = width / 2
    val sideMultiplier = if(isLeft) -1 else 1

    val bottomCenterX =
        port.position.x + distance * cos(angle) + (sideMultiplier * halfWidth * sin(angle))
    val bottomCenterY =
        port.position.y + distance * sin(angle) - (sideMultiplier * halfWidth * cos(angle))

    val rectLowerLeftX = bottomCenterX - halfWidth * cos(angle)
    val rectLowerLeftY = bottomCenterY - halfWidth * sin(angle)

    return Rectangle(rectLowerLeftX, rectLowerLeftY, width, height)
}

fun createBuildings(
    pathSegments: List<PathSegment>,
    numBuildings: Int = pathSegments.size,
    avoidCrossTol: Double = 0.1,
    seed: Int = 42
): List<Building> {
    val buildings = mutableListOf<Building>()

    val rectangleDistanceFromSegment = 2.0  // distance of rectangle from pathSegment
    val r = Random(seed)

    val mapCenter =
        pathSegments.map { it.from.position.x }.average() to pathSegments.map { it.from.position.y }.average()
    val rangeX = pathSegments.map { it.from.position.x }.run { max() - min() }
    val rangeY = pathSegments.map { it.from.position.y }.run { max() - min() }

    val centerThreshold = rangeX * 0.2
    val outsideThreshold = rangeY * 0.7

    repeat(numBuildings) {
        val segment = pathSegments.random(r)
        val mapCenterDistance =
            sqrt(
                (segment.from.position.x - mapCenter.first).pow(2.0) + (segment.from.position.y - mapCenter.second).pow(
                    2.0
                )
            ) + r.nextDouble(
                -10.0, 10.00
            ) // Euclidean distance from origin


        val buildingType = when {
            mapCenterDistance <= centerThreshold -> BuildingType.Business
            mapCenterDistance >= outsideThreshold -> BuildingType.Factory
            else -> BuildingType.Home
        }

        val buildingId = "building_${it}"

        require(avoidCrossTol < 0.5 && avoidCrossTol > 0) {
            "building position tolerance should be in [0, 0.5] but was $avoidCrossTol"
        }

        val port = Port(buildingId, trimValue(r.nextDouble(1.0), avoidCrossTol), segment)


        val area = createRectangle(port, 0.5, 6.0, 5.0, true)
        // todo try facing away from the end of the segment (to avoid overlap with crossings)
//        val rectanglePosition = segment.to.position.rotate(segment.from.position, -90.0)
//            .normalize()
//            .scale(rectangleDistanceFromSegment) // a position that is rectangleDistanceFromSegment away from the path segment
        val building = Building(buildingId, area, port, buildingType)

        buildings.add(building)
    }

    return buildings
}


data class CityMap(val roads: List<PathSegment>, val buildings: List<Building> = listOf()) {

    fun exportCsv(basePath: Path) {
        roads.toDataFrame()
            .unfold<Node>("from", addPrefix = true)
            .unfold<Node>("to", addPrefix = true)
            .unfoldByProperty<Point>("from_position", listOf(Point::x, Point::y), addPrefix = true)
            .unfoldByProperty<Point>("to_position", listOf(Point::x, Point::y), addPrefix = true)
            .renameToSnakeCase()
            .writeCSV(basePath.withExtension("segments.csv").toFile())

        buildings.toDataFrame()
            .unfold<Port>("port", addPrefix = true)
            .unfoldByProperty<Point>("port_position", listOf(Point::x, Point::y), addPrefix = true)
            .renameToSnakeCase()
            .writeCSV(basePath.withExtension("buildings.csv").toFile())
    }

    // shouldn't we better use inheritance here?
    fun toGeoMap(): GeoMap {
        val nodes = (roads.map { it.from } + roads.map { it.to }).toSet()
        return GeoMap(roads, nodes, buildings.map { it.port })
    }
}

fun buildCity(
    xBlocks: Int = 2,
    yBlocks: Int = 2,
    numBuildings: Int = 0,
): CityMap {
    val grid: List<PathSegment> = createPathGrid(xBlocks, yBlocks, dropProportion = 0.0)
    val buildings: List<Building> = createBuildings(grid, numBuildings = numBuildings)

    return CityMap(grid, buildings)
}

// highways
fun main() {
    val city = buildCity(16, 12, 100)

    city.exportCsv(Path.of("city_map"))
}