package org.kalasim.animation.examples.elevator

import kotlinx.coroutines.*
import org.kalasim.ClockSync
import org.kalasim.TickTransform
import org.kalasim.animation.lazyElevator
import org.kalasim.examples.elevator.Elevator
import org.kalasim.misc.DependencyContext
import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.olive.Olive
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.toKotlinDuration



fun main() {
    application {
        val CAR_MOVEMENT = "LiftInMotion"

//        val elevator = lazyElevator

        var frameCounter = 0

        configure {
            width = 1024
            height = 800
            windowResizable = true
            title = "Office Tower"
        }

        program {
            val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)
//            val font =


            // https://github.com/openrndr/openrndr-examples/blob/master/src/main/kotlin/examples/10_OPENRNDR_Extras/C08_Quick_UIs001.kt
            val gui = GUI()

            val settings = object {
                @IntParameter("# elevators", 1, 10)
                var numElevators: Int = 3

                @DoubleParameter("x", 0.0, 770.0)
                var x: Double = 385.0
            }

            // -- this is why we wanted to keep a reference to gui
            gui.add(settings, "Settings")


            extend(gui)

            extend(Olive<Program>())

            // -- pitfall: the extend has to take place after gui is populated

//        extend(ScreenRecorder())

            extend {
//                // draw background
////            drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE) * invert
////                drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())
//
//                val shaftNo = elevator.cars.withIndex().map{ it.value to it.index}.toMap()
//
//                with(drawer) {
//
//                    elevator.cars.forEach {
//                        val shaftOffset =  (it.capacity) * (elevator.cars.size+3)
//                        fill = ColorRGBa.YELLOW
//                        scale(10.0)
//                        rectangle(shaftOffset.toDouble(), it.currentPosition.y, 1.0, (it.capacity*10).toDouble())
////                        Rectangle.
//                    }
//
////                    // draw deposits
////                    hydProd.map.deposits.forEach {
////                        defaults()
////                        val isKnown = hydProd.base.knownDeposits.contains(it)
////                        drawer.fill = ColorRGBa.YELLOW.copy(a = if(isKnown) 0.8 else 0.2 )
////
////                    elevator.car
//////                    scale(0.3)
////                        circle(
////                            it.gridPosition.mapCoordinates.x * xScale,
////                            it.gridPosition.mapCoordinates.y * yScale,
////                            it.level / 20.0
////                        )
////                    }
////
////                    // draw harvesters
////                    hydProd.harvesters.withIndex().forEach {(idx, harvester) ->
////                        defaults()
////                        val hPos = harvester.currentPosition
//////                    val offset = if(hPos == hydProd.base.position.mapCoordinates) idx*30 else 0
////                        translate(hPos.x * xScale, hPos.y * yScale)
////
//////                        drawer.fill = ColorRGBa.BLACK
//////                        drawer.fontMap = font
//////                        drawer.text("${harvester.tank.level.toInt()}", y=80.0)
//////                        drawer.text("${harvester.holdProgress(MINING_PROGRESS)?.round(2)}", y=80.0)
////
////
////                        // draw harvester
////                        scale(0.3)
////                        composition(truck)
////
////                        // indicate tank status
////                        drawer.fill = null
////                        drawer.stroke = ColorRGBa.BLACK
////                        drawer.strokeWeight = 10.0
////
////
////                        val contour = Circle(
//////                                harvester.gridPosition.mapCoordinates.x * xScale,
//////                                harvester.gridPosition.mapCoordinates.y * yScale,
////                            100.0, 110.0,
////                            120.0
////                        )
//////                        .contour.sub(0.0, 0.5 + 0.50 * sin(seconds))
////                            .contour
////                        if(harvester.isHolding(UNLOADING_HARVESTER)) {
////                            drawer.contour(contour.sub(0.0, (1 - harvester.holdProgress(UNLOADING_HARVESTER)!!)))
////                        }else{
////                            drawer.contour(contour.sub(0.0, 1-harvester.tank.occupancy))
////                        }
////                    }
////
////                    // draw base
////                    defaults()
////                    val baseCoordinates = hydProd.base.position.mapCoordinates
////                    translate(baseCoordinates.x * xScale, baseCoordinates.y * yScale-90)
////                    scale(0.1)
////                    composition(base)
//
//
//                    // draw info
//                    defaults()
//                    drawer.fill = ColorRGBa.WHITE
//                    drawer.fontMap = font
//                    drawer.text("NOW: ${elevator.now}", width - 150.0, height - 30.0)
//                    drawer.text("Frame: ${frameCounter++}", width - 150.0, height - 50.0)
//                }
            }
        }
    }
}
