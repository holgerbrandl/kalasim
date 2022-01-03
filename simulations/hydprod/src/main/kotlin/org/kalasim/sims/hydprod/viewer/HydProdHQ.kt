package org.kalasim.sims.hydprod.viewer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kalasim.ClockSync
import org.kalasim.misc.DependencyContext
import org.kalasim.seconds
import org.kalasim.sims.hydprod.HydProd
import org.kalasim.sims.hydprod.mapCoordinates
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.svg.loadSVG

fun main() = application {

    val hydProd = HydProd().apply {
        ClockSync(tickDuration = 1.seconds, syncsPerTick = 10)

//        addEventListener<PersonStatusEvent> {
//            now = it.time
//            currentPopulation[it.person] = it
//        }
    }

    // Start simulation model
    CoroutineScope(Dispatchers.Default).launch {
        DependencyContext.setKoin(hydProd.getKoin())
        println("starting simulation")
        hydProd.run()
        println("finished simulation")
    }

    var counter = 0

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


        val xScale = width.toDouble() / hydProd.map.upperRight.mapCoordinates.x
        val yScale = height.toDouble() / hydProd.map.upperRight.mapCoordinates.y

        extend {
//            drawer.drawStyle.colorMatrix = grayscale(1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0) * invert
//            drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE) * invert
            drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())

            drawer.defaults()

////            drawer.fontMap = font
//            drawer.drawStyle.colorMatrix = Matrix55.IDENTITY
//            drawer.fill = ColorRGBa.WHITE
//            drawer.stroke = ColorRGBa.YELLOW
//            drawer.translate(200.0, 200.0)
//            drawer.text("OPENRNDR", width / 2.0, height / 2.0)

//        }

            with(drawer) {

                // draw deposits
                hydProd.map.deposits.forEach {
                    defaults()
                    drawer.fill = ColorRGBa.YELLOW
//                    scale(0.3)
                    circle(
                        it.gridPosition.mapCoordinates.x * xScale,
                        it.gridPosition.mapCoordinates.y * yScale,
                        it.level / 20.0
                    )
                }

                // draw harvesters
                hydProd.harvesters.forEach {
                    defaults()
                    translate(it.gridPosition.mapCoordinates.x * xScale, it.gridPosition.mapCoordinates.y * yScale)
                    scale(0.3)
                    composition(truck)
                }


                // draw base
                defaults()
                val baseCoordinates = hydProd.base.position.mapCoordinates
                translate(baseCoordinates.x * xScale, baseCoordinates.y * yScale)
                scale(0.1)
                composition(base)


                // draw info
                drawer.fill = ColorRGBa.WHITE
                drawer.text("NOW: ${hydProd.now}", width - 100.0, height - 50.0)
                drawer.text("Frame: ${counter++}", width - 100.0, height - 70.0)
            }
        }
    }
}