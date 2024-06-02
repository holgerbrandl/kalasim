package org.kalasim.logistics

import io.github.oshai.kotlinlogging.KotlinLogging
import org.kalasim.Component
import org.kalasim.animation.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val logger = KotlinLogging.logger {}


class CollisionSampler(val samplingRate: Duration = 1.seconds) : Component() {

    override fun repeatedProcess() = sequence {
        hold(samplingRate)

        get<PathOccupancyTracker>().roadOccupancy
            .filterValues { it.isNotEmpty() }
            .forEach { (dirSegment, cars) ->
                val vehiclePositions = cars.map { Point(it.currentPosition.x, it.currentPosition.y) }

                logger.info { "minimal vehicle distance on $dirSegment is ${vehiclePositions.minimalDistance()}" }

                require(!vehiclePositions.hasCollision()) {
                    "collision detected via sampling on $dirSegment for $cars"
                }
            }
    }
}
