package org.kalasim.logistics

import org.kalasim.Component
import org.kalasim.animation.*
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

class CollisionSampler : Component() {

    override fun repeatedProcess() = sequence {
        hold(5.seconds)

        get<PathOccupancyTracker>().roadOccupancy.filterValues { it.isNotEmpty() }.forEach { dirSegment, cars ->
            val vehiclePositions = cars.map { Point(it.currentPosition.x, it.currentPosition.y) }

            logger.info { "minimal vehcile distance on $dirSegment is ${vehiclePositions.minimalDistance()}" }

            require(!vehiclePositions.hasCollision()) {
                "collision detected via sampling on $dirSegment for $cars"
            }
        }
    }
}


fun List<Point>.hasCollision(dist: Distance = 1.meters) =
    any { point -> any { it != point && it.distance(point) < dist.meters.toDouble() } }


fun List<Point>.minimalDistance(): Double {
    return if(size < 2) 0.0 else withIndex().flatMap { (i, a) ->
        drop(i + 1).map { b -> sqrt((a.x - b.x).pow(2.0) + (a.y - b.y).pow(2.0)) }
    }.minOrNull() ?: Double.MAX_VALUE
}