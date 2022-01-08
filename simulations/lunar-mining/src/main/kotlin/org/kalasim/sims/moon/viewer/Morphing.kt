package org.kalasim.sims.moon.viewer

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.*
import java.lang.Math.sin

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val circlePoints = Circle(width / 2.0, height / 2.0, 200.0).contour.equidistantPositions(20)
        val squarePoints = Rectangle.fromCenter(Vector2(width / 2.0), 300.0, 300.0).contour.equidistantPositions(20)

        extend {

            drawer.clear(ColorRGBa.WHITE.shade(0.8))
            drawer.stroke = null
            drawer.fill = ColorRGBa.BLACK

            val blend = sin(seconds * 2.0) * 0.5 + 0.5
            val newPoints = (circlePoints zip squarePoints).map {
                it.first * blend + it.second * (1.0 - blend)
            }

            drawer.circles(newPoints, 3.0)

            drawer.strokeWeight = 2.0
            drawer.stroke = ColorRGBa.GREEN
            drawer.lineLoop(newPoints)
        }
    }
}