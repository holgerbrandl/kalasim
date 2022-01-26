package org.kalasim.animation.examples.elevator

import kotlinx.coroutines.*
import org.kalasim.ClockSync
import org.kalasim.TickTransform
import org.kalasim.examples.elevator.*
import org.kalasim.misc.DependencyContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.project
import org.openrndr.shape.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds


fun main() = application {
    // Setup simulation model
    val elevator = Elevator().apply {
        ClockSync(tickDuration = 100.milliseconds, syncsPerTick = 10)
        tickTransform = TickTransform(TimeUnit.SECONDS)

        CoroutineScope(Dispatchers.Default).launch {
            DependencyContext.setKoin(getKoin())
            run()
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

        // https://github.com/openrndr/openrndr-examples/blob/master/src/main/kotlin/examples/10_OPENRNDR_Extras/C08_Quick_UIs001.kt
        val gui = GUI()

        val settings = object {
            @IntParameter("# elevators", 1, 10)
            var numElevators: Int = 3

            @DoubleParameter("x", 0.0, 770.0)
            var x: Double = 385.0
        }

        gui.add(settings, "Settings")

        extend(gui)
        gui.visible = false

        //        extend(ScreenRecorder())

        fun Drawer.renderTextInRect(rect: Rectangle, someText: String) {
            // draw the visitor number
            val lowerLeft = Vector3(rect.x + .1, rect.y + rect.height * 0.8, 0.0)
            val projectLL = project(lowerLeft, projection, view * model, width, height)

            // keep a ref to the model, unproject rectangle position and plot
            val trafoModel = model
            defaults()

            strokeWeight = 1.0
            fill = ColorRGBa.WHITE
            fontMap = font
            text(someText, projectLL.x, projectLL.y)

            // restore model and draw other stuff in grid coordinates
            model = trafoModel
        }


        fun Drawer.drawVisitor(visitor: Visitor, gridX: Number, gridY: Number) {
            fill = ColorRGBa.GRAY
            stroke = ColorRGBa.GRAY

            val visitorRect = Rectangle(
                gridX.toDouble() + .1,
                gridY.toDouble() - 0.9,
                0.8,
                0.8
            )

            rectangle(visitorRect)
            renderTextInRect(visitorRect, visitor.toFloor.level.toString())
        }

        extend {
            val gridUnitScaleX = width / 25.0
            val gridUnitScaleY = height / 25.0

            val NUM_VISIBLE_WAITERS = 4

            with(drawer) {
                // establish a grid system
                translate(gridUnitScaleX * 8, height * 0.9)

                scale(gridUnitScaleX, gridUnitScaleY)

                // render the floors
                elevator.floors.forEach { floor ->
                    fontMap = font
                    stroke = ColorRGBa.WHITE

                    val boundBox = Rectangle(-8.0, -floor.level.toDouble() - 1, 1.0, 0.9)
                    renderTextInRect(boundBox, floor.level.toString())

                    // todo restore font and stroke along with projection
                    fontMap = font
                    stroke = ColorRGBa.WHITE

                    // also render num extra-waiting
                    val numWaiting = floor.queue.size

                    if(numWaiting > NUM_VISIBLE_WAITERS) {
                        val floorInfo = "+${numWaiting - NUM_VISIBLE_WAITERS}"
                        val waitingBBox = Rectangle(-6.0, -floor.level.toDouble(), 1.0, 1.0)
                        renderTextInRect(waitingBBox, floorInfo)
                    }
                }

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
//                            rectangle((-idx - 3), -floor.level - 1 + .1, 0.8, 0.8)
                        drawVisitor(cqe.component, -idx - 3, -floor.level)
                    }
                }


                //visualize requests
                elevator.requests.keys.toSet().forEach { (floor, direction) ->
                    strokeWeight = 0.0

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

                // draw cars
                elevator.cars.withIndex().forEach { (shaftIndex, car) ->
                    val shaftWidth = car.capacity + 1

                    fill = ColorRGBa.RED
                    stroke = ColorRGBa.RED

                    val shaftX = shaftIndex.toDouble() * (shaftWidth + 0.5)
                    val shaftY = -car.currentPosition.y
                    rectangle(
                        shaftX,
                        shaftY - 1,
                        shaftWidth.toDouble(),
                        1.0
                    )


                    // draw customers on the in the cab
                    car.visitors.components.withIndex().forEach { (idx, visitor) ->
                        val shaftVisitorX = shaftX + idx
                        drawVisitor(visitor, shaftVisitorX, shaftY)
                    }
                }

                fontMap = font
                fill = ColorRGBa.WHITE

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


fun triangle(isDown: Boolean) = contour {
    val baseLine = if(isDown) 0.1 else -0.1

    moveTo(0.1, baseLine)
    lineTo(0.9, baseLine)
    lineTo(0.5, if(isDown) 0.3 else -0.3)
    lineTo(0.1, baseLine)

    close()
}

fun Drawer.rectangle(x: Number, y: Number, width: Number, height: Number) =
    rectangle(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
