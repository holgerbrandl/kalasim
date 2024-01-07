package org.kalasim.animation.examples.traffic

import org.kalasim.Component
import org.kalasim.animation.kmh
import org.kalasim.logistics.*
import kotlin.time.Duration.Companion.seconds

object RoadAnim {
    @JvmStatic
    fun main(args: Array<String>) {
        animateCrossing {
            generateRoadWithBuildings(2, 2).apply {

                val roadDict = segments2buildings()

                addCar(object : Vehicle(roadDict[0].second[0].port) {
                    override fun process() = sequence<Component> {
                        hold(2.seconds)
                        moveTo(roadDict[1].second[0].port)
                    }
                })

                addCar(object : Vehicle(roadDict[0].second[1].port, maxSpeed = 30.kmh) {
                    override fun process() = sequence<Component> {
                        hold(5.seconds)
                        moveTo(roadDict[1].second[1].port)
                    }
                })
            }
        }
    }
}