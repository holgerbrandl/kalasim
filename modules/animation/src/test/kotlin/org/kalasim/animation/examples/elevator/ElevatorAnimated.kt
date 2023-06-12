package org.kalasim.animation.examples.elevator

import kotlinx.coroutines.*
import org.kalasim.*
import org.kalasim.animation.*
import org.kalasim.examples.elevator.*
import org.kalasim.misc.DependencyContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.project
import org.openrndr.shape.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


fun main() = application {

    var frameCounter = 0
    var elevator: Elevator = startSimulation(Elevator(), 50.milliseconds)


    // see also https://openrndr.slack.com/archives/CBJGUKVSQ/p1641933241043200
    val sideBarWidth = 200
    configure {
        width = 1024 + sideBarWidth
        height = 800
        windowResizable = true
        title = "Office Tower"
    }


    program {
        val image = loadImage("src/test/resources/Campus_Tower_Frankfurt.jpg")

        val font = loadFont("file:src/test/resources/IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

//        extend(ScreenRecorder())

        // https://github.com/openrndr/openrndr-examples/blob/master/src/main/kotlin/examples/10_OPENRNDR_Extras/C08_Quick_UIs001.kt
        val gui = GUI()

        val settings = object {
            @IntParameter("# Floors", 2, 20, order = 0)
            var topFloors: Int = 10

            @IntParameter("# elevators", 1, 6, order = 10)
            var numElevators: Int = 3

            @IntParameter("Car Capacity", 1, 8, order = 20)
            var capacity: Int = 4

            @IntParameter("Load 0->N", 0, 200, order = 30)
            var load0N: Int = 50

            @IntParameter("Load N->N", 0, 200, order = 40)
            var loadNN: Int = 100

            @IntParameter("Load N->0", 0, 200, order = 50)
            var loadN0: Int = 50
        }

        val simSettings = object {
            @IntParameter("Speed", 0, 100, order = 0)
            var speed: Int = 50

            @ActionParameter("Reset")
            fun restartModel() {
                resetModel()
            }

            fun resetModel() {
                println("file saved!")
//                elevator.stopSimulation()
                elevator.get<AsyncAnimationStop>().stop()

                // Setup new simulation model
                elevator = startSimulation(
                    with(settings) {
                        Elevator(false, load0N, loadNN, loadN0, capacity, numElevators, topFloors)
                    },
                    tickMillis = ((100 - speed + 1) / 1.5).milliseconds
                )
            }
        }


        gui.onChange { name, value ->
            println("restarting sim...")
            simSettings.restartModel()
        }

        gui.compartmentsCollapsedByDefault = false
        gui.add(settings, "Elevator Settings")
        gui.add(simSettings, "Simulation Settings")


        extend(gui)


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
            fill = when(visitor.direction) {
                Direction.DOWN -> ColorRGBa.GREEN
                Direction.STILL -> ColorRGBa.YELLOW
                Direction.UP -> ColorRGBa.RED
            }
            stroke = fill

            val visitorRect = Rectangle(
                gridX.toDouble() + .1,
                gridY.toDouble() - 0.9,
                0.8,
                0.8
            )

            rectangle(visitorRect)
            renderTextInRect(visitorRect, visitor.toFloor.level.toString())
        }

        val floorWidth = 6

        extend {
            drawer.image(image, sideBarWidth.toDouble(), 0.0, width.toDouble() - sideBarWidth, height.toDouble())
            drawer.translate(sideBarWidth.toDouble() + 20, 0.0)

            val gridUnitScaleX = width / 25.0
            val gridUnitScaleY = height / 25.0

            val NUM_VISIBLE_WAITERS = floorWidth - 2

            with(drawer) {
                // establish a grid system
                translate(gridUnitScaleX * floorWidth, height * 0.9)

                scale(gridUnitScaleX, gridUnitScaleY)

                // render the floors
                elevator.floors.forEach { floor ->
                    fontMap = font
                    stroke = ColorRGBa.WHITE

                    // show floor number
                    val boundBox = Rectangle(-floorWidth.toDouble(), -floor.level.toDouble() - 1, 1.0, 0.9)
                    renderTextInRect(boundBox, floor.level.toString())

                    // also render waiters counts (if too many)
                    val numWaiting = floor.queue.size
                    if(numWaiting > NUM_VISIBLE_WAITERS - 1) {
                        val floorInfo = "+${(numWaiting - NUM_VISIBLE_WAITERS + 1)}"
                        val waitingBBox = Rectangle(-(floorWidth - 1.0), -floor.level.toDouble() - 1, 1.0, 1.0)
                        renderTextInRect(waitingBBox, floorInfo)
                    }

                    // render the floor grounds
                    val floorGround = floor.level.toDouble()
                    strokeWeight = 0.1
                    stroke = ColorRGBa.WHITE
                    lineSegment(-floorWidth.toDouble(), -floorGround, -1.0, -floorGround)

                    // draw queue in front of elevator
                    val visitorsRects = NUM_VISIBLE_WAITERS - if(floor.queue.size > NUM_VISIBLE_WAITERS) 1 else 0
                    cmeGuard { floor.queue.asSortedList() }.take(visitorsRects).withIndex().forEach { (idx, cqe) ->
                        strokeWeight = 0.0
                        drawVisitor(cqe.component, -idx - 2, -floor.level)
                    }
                }

                // render the ceiling
                strokeWeight = 0.1
                stroke = ColorRGBa.WHITE
                lineSegment(
                    -floorWidth.toDouble(),
                    -elevator.floors.size.toDouble(),
                    -1.0,
                    -elevator.floors.size.toDouble()
                )

                //visualize requests
                //todo https://stackoverflow.com/questions/48777744/thread-safe-way-to-copy-a-hashmap
                elevator.requests.asyncCopy().keys.forEach { (floor, direction) ->
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
                    val shaftWidth = car.capacity

                    fill = ColorRGBa.BLUE
                    stroke = ColorRGBa.BLUE

                    val shaftX = shaftIndex.toDouble() * (shaftWidth + 0.2)
                    val shaftY = -car.currentPosition.y

                    rectangle(shaftX, shaftY - 1, shaftWidth.toDouble(), 1.0)

                    // draw customers on the in the cab
                    car.visitors.components.asyncCopy().withIndex().forEach { (idx, visitor) ->
                        val shaftVisitorX = shaftX + idx
                        drawVisitor(visitor, shaftVisitorX, shaftY)
                    }
                }

                fontMap = font
                fill = ColorRGBa.WHITE

                // draw time & info
                defaults()
                fill = ColorRGBa.WHITE
                fontMap = font
                text("Time: ${elevator.now}", width - 150.0, height - 10.0)
                text("Frame: ${frameCounter++}", width - 150.0, height - 30.0)
            }
        }
    }
}


fun startSimulation(elevator: Elevator, tickMillis: Duration = 50.milliseconds): Elevator {
    return elevator.apply {
        ClockSync(tickDuration = tickMillis, syncsPerTick = 10)

        dependency { AsyncAnimationStop() }

        CoroutineScope(Dispatchers.Default).launch {
            DependencyContext.setKoin(getKoin())
            run()
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
