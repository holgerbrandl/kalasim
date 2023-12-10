package org.kalasim.logistics

import com.github.holgerbrandl.kdfutils.renameToSnakeCase
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.util.unfold
import org.jetbrains.kotlinx.dataframe.api.util.unfoldByProperty
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.animation.*
import org.kalasim.misc.withExtension
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToInt
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

fun computeBuildingArea(
    port: Port,
    distance: Double,
    width: Double = 5.0,
    height: Double = 3.0,
    sideSwitch: Boolean
): Rectangle {
    val isVerticalSeg = abs(port.segment.from.x - port.segment.to.x) < 1E-5

    return if(isVerticalSeg) {
        val x0 = port.position.x + (if(sideSwitch) distance else -1 * distance)
        val x1 = x0 + (if(sideSwitch) height else -1 * height)
        Rectangle(Point(x0, port.position.y - width / 2), Point(x1, port.position.y + width / 2))
    } else {
        val y0 = port.position.y + (if(sideSwitch) distance else -1 * distance)
        val y1 = y0 + (if(sideSwitch) height else -1 * height)
        Rectangle(Point(port.position.x - width / 2, y0), Point(port.position.x + width / 2, y1))
    }

}


fun createBuildings(
    pathSegments: List<PathSegment>,
    numBuildings: Int = pathSegments.size,
    avoidCrossTol: Double = 0.1,
    seed: Int = 42
): List<Building> {
    val buildings = mutableListOf<Building>()

//    val rectangleDistanceFromSegment = 2.0  // distance of rectangle from pathSegment
    val r = Random(seed)

    val mapCenter = Point(
        pathSegments.map { it.from.position.x }.average(),
        pathSegments.map { it.from.position.y }.average()
    )
    val range = pathSegments.maxOf { (Point(0, 0) - it.from.position).meters }.meters

    val centerThreshold = range * 0.2
    val outsideThreshold = range * 0.6

    repeat(numBuildings) {
        val segment = pathSegments.random(r)
        val mapCenterDistance =
            segment.from.position - mapCenter + r.nextDouble(-10.0, 10.00).meters // Euclidean distance from origin


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

        val area = computeBuildingArea(port, 0.5, 6.0, 5.0, r.nextBoolean())
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