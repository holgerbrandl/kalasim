package org.kalasim.animation

import kotlinx.coroutines.*
import org.kalasim.*
import org.kalasim.misc.DependencyContext
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.svg.loadSVG
import java.awt.geom.Point2D
import java.lang.Thread.sleep
import kotlin.time.DurationUnit
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    application {
        // setup simulation model
        val sim = object : Environment(tickDurationUnit = DurationUnit.SECONDS) {
            init {
                ClockSync(tickDuration = 10.milliseconds, syncsPerTick = 100)
            }

            // instantiate components (not fully worked out here)
            val worker = AnimationComponent(Point2D.Double(1.0, 3.0))
        }

        // configure the window
        configure {
            width = 1024
            height = 800
            windowResizable = true
            title = "Simulation Name"
        }

        var frameCounter = 0

        program {
            // load resources such as images
            val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")
            val truck = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
            val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

            // optionally enable video recording
//            extend(ScreenRecorder())

            extend {
                // draw background
                drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())

                // visualize simulation entities
                with(drawer) {
                    val workerPosition = sim.worker.currentPosition
                    circle(workerPosition.x, workerPosition.y, 10.0)
                }


                // draw info & statistics
                drawer.defaults()
                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = font
                drawer.text("NOW: ${sim.now}", width - 150.0, height - 30.0)
                drawer.text("Frame: ${frameCounter++}", width - 150.0, height - 50.0)
            }
        }

        // Start simulation model
        CoroutineScope(Dispatchers.Default).launch {
            //rewire koin context for dependency injection to async execution context
            DependencyContext.setKoin(sim.getKoin())
            // wait because Openrndr needs a second to warm up
            sleep(3000)
            sim.run()
        }
    }
}
