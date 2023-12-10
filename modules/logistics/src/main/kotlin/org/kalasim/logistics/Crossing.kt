package org.kalasim.logistics

import org.kalasim.*
import org.kalasim.animation.kmh
import kotlin.time.Duration.Companion.days


class PathOccupancyTracker {
    fun enteringSegment(vehicle: Vehicle, directedSegment: DirectedPathSegment) {
        roadOccupancy.toList().firstOrNull { it.second.contains(vehicle) }?.second?.remove(vehicle)

        (findVehicles(directedSegment) as MutableList<Vehicle>).add(vehicle)
    }

    fun findVehicles(directedSegment: DirectedPathSegment): List<Vehicle> =
        roadOccupancy.getOrPut(directedSegment) { mutableListOf() }

    val roadOccupancy = mutableMapOf<DirectedPathSegment, MutableList<Vehicle>>()
}


open class Crossing(numVehicles: Int = 0, cityMap: CityMap) : Environment() {
    fun buildingMap(): List<PathSegment> {
        TODO("Not yet implemented")
    }

    constructor(numVehicles: Int = 2, xBlocks: Int = 2, yBlocks: Int = 2, numBuildings: Int = 10) : this(
        numVehicles,
        buildCity(xBlocks, yBlocks, numBuildings = numBuildings)
    )

    val cityMap: CityMap = dependency { cityMap }

    val geomMap = dependency { cityMap.toGeoMap() }

    init {
        dependency { PathOccupancyTracker() }
        dependency { PathFinder(geomMap) }
    }

    val cars = mutableListOf<Vehicle>()

    fun addCar(vehicle: Vehicle) = cars.add(vehicle)

    init {
        if(numVehicles > 0) {
            List(numVehicles) {
                RandomMoveCar(
                    cityMap.buildings.random(random),
                    random.nextInt(50, 150).kmh
                )
            }.apply { cars.addAll(this) }
        }
    }
}

fun Crossing.segments2buildings() = cityMap.buildings.groupBy { it.port.segment }
    .mapValues { (seg, buildings) -> buildings.sortedBy { it.port.distance } }
    .toList()


fun main() {
    val sim = Crossing(2).apply {
        enableComponentLogger()
    }

    sim.run(100.days)
}
