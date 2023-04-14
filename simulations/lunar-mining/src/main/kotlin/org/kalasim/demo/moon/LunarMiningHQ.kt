package org.kalasim.sims.lunarmining.viewer

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.kalasim.ClockSync
import org.kalasim.Component
import org.kalasim.demo.moon.*
import org.kalasim.misc.DependencyContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG
import java.lang.Thread.sleep
import kotlinx.datetime.Instant
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun main() {
    application {
        val UNLOADING_HARVESTER = "Unloading"

        val lumi = LunarMining().apply {
            ClockSync(tickDuration = 6.milliseconds, syncsPerTick = 100)

            // configure harvesters to track mining events
            harvesters.forEach {
                it.registerHoldTracker(UNLOADING_HARVESTER) {
                    description?.run { startsWith("Unloading") && endsWith("water units") } ?: false
                }
            }
        }

        var frameCounter = 0

        configure {
            width = 1024
            height = 800
            windowResizable = true
            title = "Lunar Water Mining"
        }

        program {
            val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")

            val truck = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
            val base = loadSVG("src/main/resources/base.svg")

            val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

            val gridUnitSize = 10

            extend(ScreenRecorder())

            extend {
                val scaledXUnit = width.toDouble() / (lumi.map.gridDimension.width * gridUnitSize)
                val scaledYUnit = height.toDouble() / (lumi.map.gridDimension.height * gridUnitSize)


                // draw background
                drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())

                with(drawer) {

                    // draw deposits
                    lumi.map.deposits.forEach {
                        defaults()
                        val isKnown = lumi.base.knownDeposits.contains(it)
                        // openrender 4.x
//                        drawer.fill = ColorRGBa.fromHex("0E86D4").copy(alpha = if(isKnown) 0.8 else 0.2)

                        // openrender 3.x
                        drawer.fill = ColorRGBa.fromHex("0E86D4").copy(a = if(isKnown) 0.8 else 0.2)

                        circle(
                            it.gridPosition.mapCoordinates.x * scaledXUnit,
                            it.gridPosition.mapCoordinates.y * scaledYUnit,
                            it.level / 20.0
                        )
                    }
//                    val foo = Clock.System.now() + 3.minutes

                    drawer.fill = ColorRGBa.BLACK
                    drawer.fontMap = font

                    val searchHistory = lumi.base.scanHistory.keys.toList()
                    for(i in 0..lumi.map.gridDimension.width)
                        for(j in 0..lumi.map.gridDimension.height)
                            if(searchHistory.contains(GridPosition(i, j))) {
                                circle(i * scaledXUnit * gridUnitSize, j * scaledYUnit * gridUnitSize, 3.0)
                            }


                    // draw harvesters
                    val posCounts = lumi.harvesters.groupingBy { it.currentPosition }.eachCount()

                    // draw harvester
                    lumi.harvesters.forEach { harvester ->
                        defaults()
                        val harvesterPosition = harvester.currentPosition
                        translate((harvesterPosition.x - 8) * scaledXUnit, (harvesterPosition.y - 8) * scaledYUnit)

                        val numHarvesters = posCounts[harvesterPosition] ?: 0
                        if(numHarvesters > 1) {
                            drawer.fill = ColorRGBa.BLACK
                            drawer.fontMap = font
                            drawer.text(numHarvesters.toString())
                        }

                        // draw the svg
                        scale(0.3)
                        composition(truck)

                        // draw loading status
                        drawer.fill = null
                        drawer.stroke = ColorRGBa.BLACK
                        drawer.strokeWeight = 10.0

                        val contour = Circle(100.0, 110.0, 120.0)
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
                    val baseCoordinates = lumi.base.position.mapCoordinates
                    translate(baseCoordinates.x * scaledXUnit, (baseCoordinates.y - 1) * scaledYUnit)

                    scale(0.1)
                    composition(base)

                    defaults()
                    translate((baseCoordinates.x + 3) * scaledXUnit, (baseCoordinates.y + 22) * scaledYUnit)
                    drawer.fill = ColorRGBa.BLACK
                    drawer.fontMap = font
                    if(!lumi.harvesters.any { it.isHolding(UNLOADING_HARVESTER) }) {
                        drawer.text(String.format("%06d", lumi.base.refinery.level.roundToInt()))
                    } else {
                        drawer.text(List(6) { Random.nextInt(9) }.joinToString(""))
                    }

                    // draw info
                    defaults()
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = font
                    drawer.text("NOW: ${lumi.now}", width - 150.0, height - 30.0)
                    drawer.text("Frame: ${frameCounter++}", width - 150.0, height - 50.0)
                }
            }
        }

        // Start simulation model
        CoroutineScope(Dispatchers.Default).launch {
            DependencyContext.setKoin(lumi.getKoin())

            // stop the simulation if all deposits are depleted
            object : Component() {
                override fun repeatedProcess() = sequence {
                    hold(1.hours)
                    if(get<DepositMap>().depletionRatio > 0.6) {
                        stopSimulation()
                        exitProcess(0)
                    }
                }
            }

            sleep(3000)
            lumi.run()
        }
    }
}
