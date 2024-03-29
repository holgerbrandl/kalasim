package org.kalasim.animation.examples.traffic

import org.kalasim.animation.*
import org.kalasim.logistics.CollisionSampler
import org.kalasim.logistics.Crossing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.DoubleParameter


fun main() = animateCrossing { Crossing(2, numBuildings = 4) }

fun animateCrossing(builder: () -> Crossing = { Crossing() }) = application {
    var frameCounter = 0
    lateinit var sim: Crossing

    // see also https://openrndr.slack.com/archives/CBJGUKVSQ/p1641933241043200
    val sideBarWidth = 200
    configure {
        width = 1024 + sideBarWidth
        height = 650
        windowResizable = true
        title = "Crossing"
    }

    program {
//        val image = loadImage("src/test/resources/Campus_Tower_Frankfurt.jpg")
        val font = loadFont("file:modules/animation/src/test/resources/IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

//        extend(ScreenRecorder())
        val gui = GUI()

        val settings = object {
//            @IntParameter("# Vehicles", 2, 20, order = 0)
//            var numVehicles: Int = 10
        }

        val simSettings = object {
            @DoubleParameter("Speed", 0.1, 10.0)
            var speed: Double = 1.0

            @ActionParameter("Reset")
            fun restartModel() {
                resetModel()
            }

            fun resetModel() {
                try {
                    sim.getOrNull<AsyncAnimationStop>()?.stop()
                } catch(_: UninitializedPropertyAccessException) {
                }

                sim = with(settings) {
                    // todo inject parameters here
                    builder().apply {
                        CollisionSampler()
                    }
                }
                // Setup new simulation model
                sim.startSimulation(speed, 1600)
            }
        }


        gui.onChange { _, _ -> simSettings.restartModel() }

        gui.compartmentsCollapsedByDefault = false
        gui.add(settings, "Model Settings")
        gui.add(simSettings, "Runtime Settings")

        extend(gui)

        val mapLimits = sim.geomMap.getLimits(0.1)

        extend {
            with(drawer) {
//                image(image, sideBarWidth.toDouble(), 0.0, width.toDouble() - sideBarWidth, height.toDouble())
                clear(ColorRGBa.BLACK)

                translate(sideBarWidth.toDouble(), 0.0)

                val gridUnitScaleX =
                    (width - sideBarWidth.toDouble()) / mapLimits.width
                val gridUnitScaleY = height / mapLimits.height

                // establish a grid system
                scale(gridUnitScaleX, gridUnitScaleY)
                translate(mapLimits.x.times(-1.0), mapLimits.y.times(-1.0))


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

                // render buildings with load-ports
                stroke = ColorRGBa.DARK_GREEN
                fill = ColorRGBa.DARK_GREEN
                sim.cityMap.buildings.forEach { building ->
                    rectangle(building.area.toOpenRendrRectangle())
                }

                stroke = ColorRGBa.DARK_GREEN
                fill = ColorRGBa.DARK_GREEN

                sim.cityMap.buildings.forEach { building ->
//                    fontMap = font
//                    text(building.id, building.port.position.x, building.port.position.y)
                    circle(building.port.position.toOpenRendrVector2(), 0.7)
                }

                stroke = ColorRGBa.LIGHT_BLUE

                sim.cars.forEach { car ->
                    rectangle(car.currentPosition.x, car.currentPosition.y, 1.5, 1.5)
                }

                fontMap = font
                fill = ColorRGBa.WHITE

                // draw time & info
                defaults()
                fill = ColorRGBa.WHITE
                fontMap = font
                text("Time: ${sim.now.format("HH:mm:ss:SSS")}", sideBarWidth.toDouble() + 10, height - 10.0)
                text("Frame: ${frameCounter++}", sideBarWidth.toDouble() + 10, height - 30.0)
            }
        }
    }
}
