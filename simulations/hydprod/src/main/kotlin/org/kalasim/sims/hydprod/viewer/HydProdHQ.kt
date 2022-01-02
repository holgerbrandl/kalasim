package org.kalasim.sims.hydprod.viewer

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix55
import org.openrndr.svg.loadSVG

fun main() = application {
    configure {
        width = 1024
        height = 800
        windowResizable = true
        title = "Hydrate Production"
    }

    program {
//        val image = loadImage("data/images/pm5544.png")
        val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")

        val truck = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
        val base = loadSVG("src/main/resources/base.svg")


        val mapScale =Math.min(width, height)/100

//        val composition = drawComposition {
//            fill = ColorRGBa.PINK
//            stroke = ColorRGBa.BLACK
//            isolated {
//                for (i in 0 until 100) {
//                    circle(Vector2(0.0, 0.0), 50.0)
//                    translate(50.0, 50.0)
//                }
//            }
//
//        }

//        val movingTruck = drawComposition{
//            stroke = ColorRGBa.BLACK
//            fill = ColorRGBa.PINK
//
//            isolated {
//                for (i in 0 until 10) {
//                    drawer.composition(truck)
//                    translate(1.0, 1.0)
//                }
//            }
//        }

        extend {
//            drawer.drawStyle.colorMatrix = grayscale(1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0) * invert
//            drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE) * invert
            drawer.image(image, 0.0,0.0, width.toDouble(), height.toDouble())

        }

        extend {
            drawer.defaults()

//            drawer.fontMap = font
            drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.YELLOW
            drawer.translate(200.0, 200.0)
            drawer.text("OPENRNDR", width / 2.0, height / 2.0)

        }

        extend{
            drawer.fill = ColorRGBa.YELLOW
            drawer.stroke = ColorRGBa.YELLOW
//            drawer.strokeWeight = 2.0

            drawer.translate(40.0*mapScale, 2.0*mapScale)
            drawer.scale(0.3)
            drawer.composition(truck)
        }

        extend{
            drawer.defaults()

            drawer.fill = ColorRGBa.YELLOW
            drawer.stroke = ColorRGBa.YELLOW

            drawer.scale(0.1)
            drawer.translate(150.0*mapScale, 200.0*mapScale)
            drawer.composition(base)


//            drawer.composition(movingTruck)
//            drawer.composition(composition)
        }
    }
}
