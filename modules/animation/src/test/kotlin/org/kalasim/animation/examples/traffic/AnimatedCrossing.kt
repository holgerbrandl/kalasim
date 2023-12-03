package org.kalasim.animation.examples.traffic

import org.kalasim.*
import org.kalasim.animation.AsyncAnimationStop
import org.kalasim.animation.startSimulation
import org.kalasim.logistics.*
import org.kalasim.logistics.examples.simpleCrossing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.LIGHT_BLUE
import org.openrndr.extra.color.presets.LIGHT_GRAY
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.IntParameter
import kotlin.time.Duration.Companion.seconds

class Crossing : Environment() {

    val roadMap = dependency { simpleCrossing() }

    init {
        enableComponentLogger()

        dependency { PathFinder(roadMap) }
    }


    class Car(startingPosition: Port) : Vehicle(startingPosition) {
        override fun repeatedProcess() = sequence {
            hold(10.seconds)
            activate(Vehicle::moveTo, get<GeoMap>().ports.random(random))
        }
    }


    val cars = List(10) {
        Car(roadMap.ports.random(random))
    }
}

fun main() = application {

    var frameCounter = 0
    var sim = Crossing()


    // see also https://openrndr.slack.com/archives/CBJGUKVSQ/p1641933241043200
    val sideBarWidth = 200
    configure {
        width = 1024 + sideBarWidth
        height = 800
        windowResizable = true
        title = "Office Tower"
    }


    program {
//        val image = loadImage("src/test/resources/Campus_Tower_Frankfurt.jpg")

        val font = loadFont("file:src/test/resources/IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

//        extend(ScreenRecorder())

        // https://github.com/openrndr/openrndr-examples/blob/master/src/main/kotlin/examples/10_OPENRNDR_Extras/C08_Quick_UIs001.kt
        val gui = GUI()

        val settings = object {
            @IntParameter("# Floors", 2, 20, order = 0)
            var topFloors: Int = 10

            @ActionParameter("save", order = 0)
            fun doSave() {
                println("file saved!")
            }
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
                sim.getOrNull<AsyncAnimationStop>()?.stop()

                sim = with(settings) {
                    Crossing()
                }
                // Setup new simulation model
                sim.startSimulation(
                    tickMillis = 1.minute
//                tickMillis = ((100 - speed + 1) / 1.5).milliseconds
                )
            }
        }


        gui.onChange { _, _ ->
            println("restarting sim...")
            simSettings.restartModel()
        }

        gui.compartmentsCollapsedByDefault = false
        gui.add(settings, "Elevator Settings")
        gui.add(simSettings, "Simulation Settings")


        extend(gui)

        extend {
            // todo bring back image
//            drawer.image(image, sideBarWidth.toDouble(), 0.0, width.toDouble() - sideBarWidth, height.toDouble())
            drawer.clear(ColorRGBa.BLACK)

            drawer.translate(sideBarWidth.toDouble() - width / 2.0, -height / 2.0)

            val gridUnitScaleX = width / 10.0
            val gridUnitScaleY = height / 10.0

            with(drawer) {
                // establish a grid system
                translate(gridUnitScaleX * 6, height * 0.9)

                scale(gridUnitScaleX, gridUnitScaleY)

                // render the ceiling
                strokeWeight = 0.2
                stroke = ColorRGBa.LIGHT_GRAY

                sim.roadMap.segments.forEach { pathSeg ->
                    val fromPos = pathSeg.from.position
                    val toPos = pathSeg.to.position
                    lineSegment(
                        toPos.x,
                        toPos.y,
                        fromPos.x,
                        fromPos.y
                    )
                }

                stroke = ColorRGBa.LIGHT_BLUE

//                sim.cars.forEach { car ->
//                    rectangle(car.currentPosition.x, car.currentPosition.y, 2.0, 2.0)
//                }

                fontMap = font
                fill = ColorRGBa.WHITE

                // draw time & info
                defaults()
                fill = ColorRGBa.WHITE
                fontMap = font
                text("Time: ${sim.now}", width - 150.0, height - 10.0)
                text("Frame: ${frameCounter++}", width - 150.0, height - 30.0)
            }
        }
    }
}

