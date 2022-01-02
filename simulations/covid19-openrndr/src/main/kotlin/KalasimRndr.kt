import org.kalasim.*
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.olive.oliveProgram

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program  {
        val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-SemiBoldItalic.ttf", 24.0)


// create simulation with no default logging
        val sim = createSimulation {
            eventLog()
            ComponentGenerator(iat = 1.asDist()) { Component("Car.${it}") }
        }

        val logs = sim.get<EventLog>()

        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = null

            // -- translate to center of screen
            drawer.translate(width / 2.0, height / 2.0)
            // -- scale around origin
            drawer.scale(Math.cos(seconds * Math.PI * 2.0) + 2.0)
            // -- rectangle around the origin
            drawer.rectangle(-50.0, -50.0, 100.0, 100.00)

            drawer.fontMap = font
            drawer.fill = ColorRGBa.GREEN
            drawer.text("Counter $seconds", width / 2.0 - 100.0, height / 2.0)
        }
    }
}