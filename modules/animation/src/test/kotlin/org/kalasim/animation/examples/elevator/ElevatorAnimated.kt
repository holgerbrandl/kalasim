package org.kalasim.animation.examples.elevator

import kotlinx.coroutines.*
import org.kalasim.ClockSync
import org.kalasim.TickTransform
import org.kalasim.examples.elevator.Direction
import org.kalasim.examples.elevator.Elevator
import org.kalasim.misc.DependencyContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.*
import org.openrndr.text.writer
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds


fun main() {
    application {
        val CAR_MOVEMENT = "LiftInMotion"

        val elevator = Elevator().apply {
            ClockSync(tickDuration = 100.milliseconds, syncsPerTick = 10)
            tickTransform = TickTransform(TimeUnit.SECONDS)

            CoroutineScope(Dispatchers.Default).launch {
                DependencyContext.setKoin(getKoin())
                println("starting simulation")
                run()
                println("finished simulation")
            }
        }


        var frameCounter = 0

        configure {
            width = 1024
            height = 800
            windowResizable = true
            title = "Office Tower"
        }

        program {
            val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)
            val fontSmall = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 8.0)

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
            gui.visible = false

//            extend(Olive<Program>())

            // -- pitfall: the extend has to take place after gui is populated

//        extend(ScreenRecorder())

            extend {

                val gridUnitScaleX = width / 25.0
                val gridUnitScaleY = height / 25.0

                val NUM_VISIBLE_WAITERS = 4

                with(drawer) {
                    // small blinking indicator in upper left
                    defaults()
                    fill = ColorRGBa.GREEN
                    if(seconds.roundToInt().mod(2) == 0)
                        rectangle(5.0, 5.0, 15.0, 15.0)

                    // render the cars
                    defaults()
                    translate(width / 3.0, height * 0.9)


                    elevator.floors.forEach { floor ->
                        fontMap = font
                        text(
                            floor.level.toString(),
                            -1.9 * gridUnitScaleX,
                            (-floor.level.toDouble() + -0.3) * gridUnitScaleY
                        )


                        // aslso render num extra-waiting
                        val numWaiting = floor.queue.size

                        if(numWaiting > NUM_VISIBLE_WAITERS) {
                            text(
                                "+${numWaiting - NUM_VISIBLE_WAITERS}",
                                -width / 3.0,
                                (-floor.level.toDouble() + -0.3) * gridUnitScaleY
                            )
                        }
                    }

                    scale(gridUnitScaleX, gridUnitScaleY)


                    // draw floors
                    elevator.floors.forEach { floor ->
                        val floorGround = floor.level.toDouble()
                        strokeWeight = 0.1
                        stroke = ColorRGBa.WHITE
                        lineSegment(-100.0, -floorGround, -2.0, -floorGround)

                    }

                    translate(0.0, 0.0)

                    // draw queue in front of elevator
                    elevator.floors.forEach { floor ->
                        floor.queue.asSortedList().take(5).withIndex().forEach { (idx, cqe) ->
                            strokeWeight = 0.0
                            fill = ColorRGBa.GRAY
                            rectangle((-idx - 3), -floor.level - 1 + .1, 0.8, 0.8)
                        }
                    }


                    //visualize requests
                    elevator.requests.toMap().keys.forEach { (floor, direction) ->
                        strokeWeight = 0.0
//                        drawer.popTransforms()
//                        ortho()
//                        translate(-1.0, floor.level.toDouble())

                        val requestIndicator = when(direction) {
                            Direction.DOWN -> {
                                fill = ColorRGBa.GREEN
                                triangle(true)
                            }
                            Direction.STILL -> {
                                ShapeContour.EMPTY
                            }
                            Direction.UP -> {
                                fill = ColorRGBa.RED
                                triangle(false)
                            }
                        }

                        contour(requestIndicator.transform(
                            buildTransform {
                                translate(-1.0, -floor.level.toDouble() - .5)
                            }
                        ))
                    }

//                    translate(0.0, 0.0)

                    // draw cars
                    elevator.cars.withIndex().forEach { (shaftIndex, car) ->
                        val shaftWidth = car.capacity + 1
                        val shaftOffset = (shaftWidth) * (elevator.cars.size + 3)
                        fill = ColorRGBa.RED
//                        stroke = ColorRGBa.RED
                        stroke = ColorRGBa.BLACK

                        rectangle(
                            shaftIndex.toDouble() * (shaftWidth + 0.5),
                            -car.currentPosition.y - 1,
                            shaftWidth.toDouble(),
                            1.0
                        )


                        // draw customers on the in the cab
                        car.visitors.components.withIndex().forEach { (idx, visitor) ->
                            fill = ColorRGBa.GRAY

                            val visitorRect = Rectangle(
                                shaftIndex.toDouble() * (shaftWidth + 0.5) + idx + .1,
                                -car.currentPosition.y - 0.9,
                                0.8,
                                0.8
                            )

                            rectangle(visitorRect)

                            // draw the visitor number
                            fill = ColorRGBa.WHITE
                            fontMap = fontSmall

                            writer {
                                box = visitorRect
                                text("2")
                            }

                        }
                    }

                    fontMap = font
                    fill = ColorRGBa.WHITE

//                    writer {
//                        box = Rectangle(-1.0, -10.0, 10.0, 10.0)
//                        text("Test")
//                    }

                    // draw info
                    defaults()
                    drawer.fill = ColorRGBa.WHITE
                    drawer.fontMap = font
                    drawer.text("NOW: ${elevator.now}", width - 150.0, height - 30.0)
                    drawer.text("Frame: ${frameCounter++}", width - 150.0, height - 50.0)
                }
            }
        }
    }
}

fun triangle(isDown: Boolean) = contour {
    val baseLine = if(isDown) 0.1 else -0.1

    moveTo(0.0, baseLine)
    lineTo(1.0, baseLine)
    lineTo(0.5, if(isDown) 0.3 else -0.3)
    lineTo(0.0, baseLine)

    close()
}

fun Drawer.rectangle(x: Number, y: Number, width: Number, height: Number) =
    rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
