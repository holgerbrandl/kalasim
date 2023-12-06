package org.kalasim.animation.examples.traffic

import org.kalasim.animation.AsyncAnimationStop
import org.kalasim.animation.startSimulation
import org.kalasim.logistics.Crossing
import org.kalasim.logistics.Rectangle
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.math.Vector2
import java.awt.geom.Point2D
import kotlin.time.Duration.Companion.seconds

fun main() = application {

    var frameCounter = 0
    var sim = Crossing()

//        sim.cityMap.exportCsv(Path.of("meinestadt"))


    // see also https://openrndr.slack.com/archives/CBJGUKVSQ/p1641933241043200
    val sideBarWidth = 200
    configure {
        width = 1024 + sideBarWidth
        height = 800
        windowResizable = true
        title = "Crossing"
    }


    program {
//        val image = loadImage("src/test/resources/Campus_Tower_Frankfurt.jpg")
        val font = loadFont("file:modules/animation/src/test/resources/IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

//        extend(ScreenRecorder())

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
                sim.startSimulation(tickDuration = 1.seconds, 200)
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


        val mapLimits = sim.geomMap.getLimits(0.4)

        extend {
            // todo bring back image
//            drawer.image(image, sideBarWidth.toDouble(), 0.0, width.toDouble() - sideBarWidth, height.toDouble())
            drawer.clear(ColorRGBa.BLACK)



            drawer.translate(sideBarWidth.toDouble(), 0.0)

            val gridUnitScaleX = (width - sideBarWidth.toDouble()) / mapLimits.width // todo use corrected width here
            val gridUnitScaleY = height / mapLimits.height

            // establish a grid system
            drawer.scale(gridUnitScaleX, gridUnitScaleY)
            drawer.translate(mapLimits.x.times(-1.0), mapLimits.y.times(-1.0))

            with(drawer) {

                // render the ceiling
                strokeWeight = 0.1
                stroke = ColorRGBa.LIGHT_GRAY

                sim.geomMap.segments.forEach { pathSeg ->
                    val fromPos = pathSeg.from.position
                    val toPos = pathSeg.to.position
                    lineSegment(
                        fromPos.x,
                        fromPos.y,
                        toPos.x,
                        toPos.y
                    )
                }


                // rendr buildings with load-ports
                sim.cityMap.buildings.forEach { building ->
                    stroke = ColorRGBa.LIGHT_GREEN
                    rectangle(building.area.toOpenRendrRect())
                }

                sim.cityMap.buildings.forEach { building ->
                    stroke = ColorRGBa.LIGHT_CORAL
                    fill = ColorRGBa.LIGHT_CORAL
                    fontMap = font
//                    text(building.id, building.port.position.x, building.port.position.y)
//                    rectangle(building.port.position.x, building.port.position.y, 6.0,6.0)
                    circle(building.port.position.toOpenRendrPoint(), 1.0)
                }

                stroke = ColorRGBa.LIGHT_BLUE

                sim.cars.forEach { car ->
                    rectangle(car.currentPosition.x, car.currentPosition.y, 1.0, 1.0)
                }

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

private fun Point2D.Double.toOpenRendrPoint(): Vector2 = Vector2(x, y)

private fun Rectangle.toOpenRendrRect(): org.openrndr.shape.Rectangle {
    return org.openrndr.shape.Rectangle(x, y, width, height)
}

