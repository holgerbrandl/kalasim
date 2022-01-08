package org.kalasim.sims.hydprod.viewer

import kotlinx.coroutines.*
import org.kalasim.ClockSync
import org.kalasim.misc.DependencyContext
import org.kalasim.sims.hydprod.GridPosition
import org.kalasim.sims.hydprod.HydProd
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    application {
//        val MINING_PROGRESS = "mining"
        val UNLOADING_HARVESTER = "Unloading"

        val hydProd = HydProd().apply {
            ClockSync(tickDuration = 10.milliseconds, syncsPerTick = 100)

            // configure harvesters to track mining events
            harvesters.forEach {
//                it.registerHoldTracker(MINING_PROGRESS){ description?.startsWith("Mining deposit") ?: false}
                it.registerHoldTracker(UNLOADING_HARVESTER) {
                    description?.run { startsWith("Unloading") && endsWith("hydrate units") } ?: false
                }
            }
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

            val gridUnitSize = 10
            val scaledXUnit = width.toDouble() / (hydProd.map.gridDimension.width * gridUnitSize)
            val scaledYUnit = height.toDouble() / (hydProd.map.gridDimension.height * gridUnitSize)


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
                        drawer.fill = ColorRGBa.YELLOW.copy(a = if(isKnown) 0.8 else 0.2)

//                    scale(0.3)
                        circle(
                            it.gridPosition.mapCoordinates.x * scaledXUnit,
                            it.gridPosition.mapCoordinates.y * scaledYUnit,
                            it.level / 20.0
                        )
                    }

                    drawer.fill = ColorRGBa.BLACK
                    drawer.fontMap = font

                    val searchHistory = hydProd.base.scanHistory.keys.toList()
                    for(i in 0..hydProd.map.gridDimension.width)
                        for(j in 0..hydProd.map.gridDimension.height)
                            if(searchHistory.contains(GridPosition(i, j))) {
                                circle(i * scaledXUnit * gridUnitSize, j * scaledYUnit * gridUnitSize, 3.0)
//                                text(".", i * scaledXUnit * gridUnitSize, j * scaledYUnit * gridUnitSize)
                            }


                    // draw harvesters
                    hydProd.harvesters.withIndex().forEach { (idx, harvester) ->
                        defaults()
                        val hPos = harvester.currentPosition
//                    val offset = if(hPos == hydProd.base.position.mapCoordinates) idx*30 else 0
                        translate((hPos.x - 8) * scaledXUnit, (hPos.y - 8) * scaledYUnit)

//                        drawer.fill = ColorRGBa.BLACK
//                        drawer.fontMap = font
//                        drawer.text("${harvester.tank.level.toInt()}", y=80.0)
//                        drawer.text("${harvester.holdProgress(MINING_PROGRESS)?.round(2)}", y=80.0)


                        // draw harvester
                        scale(0.3)
                        composition(truck)

                        // indicate tank status
                        drawer.fill = null
                        drawer.stroke = ColorRGBa.BLACK
                        drawer.strokeWeight = 10.0


                        val contour = Circle(
//                                harvester.gridPosition.mapCoordinates.x * xScale,
//                                harvester.gridPosition.mapCoordinates.y * yScale,
                            100.0, 110.0,
                            120.0
                        )
//                        .contour.sub(0.0, 0.5 + 0.50 * sin(seconds))
                            .contour
                        if(harvester.isHolding(UNLOADING_HARVESTER)) {
                            drawer.contour(contour.sub(0.0, (1 - harvester.holdProgress(UNLOADING_HARVESTER)!!)))
                        } else {
                            drawer.contour(contour.sub(0.0, 1 - harvester.tank.occupancy))
                        }
                    }

                    // draw base
                    defaults()
                    val baseCoordinates = hydProd.base.position.mapCoordinates
                    translate(baseCoordinates.x * scaledXUnit, baseCoordinates.y * scaledYUnit - 90)
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
}
