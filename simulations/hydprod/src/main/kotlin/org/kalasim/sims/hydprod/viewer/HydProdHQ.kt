package org.kalasim.sims.hydprod.viewer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kalasim.ClockSync
import org.kalasim.misc.DependencyContext
import org.kalasim.sims.hydprod.HydProd
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.svg.loadSVG
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

fun main() = application {

    val hydProd = HydProd().apply {
        ClockSync(tickDuration = 1.milliseconds, syncsPerTick = 100)
    }

    // Start simulation model
    CoroutineScope(Dispatchers.Default).launch {
        DependencyContext.setKoin(hydProd.getKoin())
        println("starting simulation")
        hydProd.run(50.days)
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
        val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")

        val truck = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
        val base = loadSVG("src/main/resources/base.svg")

        val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

        val xScale = width.toDouble() / (hydProd.map.gridDimension.width*10)
        val yScale = height.toDouble() / (hydProd.map.gridDimension.height*10)

//        extend(ScreenRecorder())

        extend {
            // draw background
//            drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE) * invert
            drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())

            with(drawer) {

                // draw deposits
                hydProd.map.deposits.forEach {
                    defaults()
                    val isKnown = hydProd.base.knownDeposits.contains(it)
                    drawer.fill = ColorRGBa.YELLOW.copy(a = if(isKnown) 0.8 else 0.2 )

//                    scale(0.3)
                    circle(
                        it.gridPosition.mapCoordinates.x * xScale,
                        it.gridPosition.mapCoordinates.y * yScale,
                        it.level / 20.0
                    )
                }

                // draw harvesters
                hydProd.harvesters.withIndex().forEach {(idx, harvester) ->
                    defaults()
                    val hPos = harvester.currentPosition
//                    val offset = if(hPos == hydProd.base.position.mapCoordinates) idx*30 else 0
                    translate(hPos.x * xScale, hPos.y * yScale)

                    drawer.fill = ColorRGBa.BLACK
                    drawer.fontMap = font
                    drawer.text("${harvester.tank.level.toInt()}", y=80.0)
                    scale(0.3)
                    composition(truck)
                }

                // draw base
                defaults()
                val baseCoordinates = hydProd.base.position.mapCoordinates
                translate(baseCoordinates.x * xScale, baseCoordinates.y * yScale-90)
                scale(0.1)
                composition(base)


                // draw info
                defaults()
                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = font
                drawer.text("NOW: ${hydProd.now}", width - 150.0, height - 30.0)
                drawer.text("Frame: ${counter++}", width - 150.0, height - 50.0)
            }
        }
    }
}
