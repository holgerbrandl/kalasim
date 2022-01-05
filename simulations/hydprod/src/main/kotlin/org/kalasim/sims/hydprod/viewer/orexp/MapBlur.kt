package org.kalasim.sims.hydprod.viewer.orexp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kalasim.ClockSync
import org.kalasim.misc.DependencyContext
import org.kalasim.sims.hydprod.HydProd
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG
import java.awt.geom.Point2D
import java.lang.Math.cos
import java.lang.Thread.sleep
import java.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

fun main() = application {

    var counter = 0

    configure {
        width = 1024
        height = 800
        windowResizable = true
        title = "Hydrate Production"
    }

    program {
        val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")

        val truck = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
        val base = loadSVG("src/main/resources/base.svg")

        val font = loadFont("file:IBM_Plex_Mono/IBMPlexMono-Bold.ttf", 24.0)

        val xScale = width.toDouble() / (image.width*10)
        val yScale = height.toDouble() / (image.height*10)

        val blur = BoxBlur()


//        extend(ScreenRecorder())

        extend {
            blur.window = (cos(seconds * Math.PI) * 4.0 + 5.0).toInt()
            val blurred = colorBuffer(image.width, image.height)

            blur.apply(image, blurred)


            // draw background
//            drawer.drawStyle.colorMatrix = tint(ColorRGBa.BLUE) * invert
            drawer.image(blurred, 0.0, 0.0, width.toDouble(), height.toDouble())

            with(drawer) {

//
//                    // visualize hold-state (except for moving)
//
//                    // if component is on hold
//                    if(harvester.isScheduled){
//
//                    }
//                    drawer.fill = null
//                    drawer.stroke = ColorRGBa.PINK
//                    drawer.strokeWeight = 4.0
//
//                    val sub0 = Circle(
//                        it.gridPosition.mapCoordinates.x * xScale,
//                        it.gridPosition.mapCoordinates.y * yScale,
//                        100.0).contour.sub(0.0, 0.5 + 0.50 * sin(seconds))
//                    drawer.contour(sub0)
//
//                }

                // draw base
                defaults()
                val baseCoordinates = Point2D.Double(100.0,100.0)
                translate(baseCoordinates.x * xScale, baseCoordinates.y * yScale-90)
                scale(0.1)
                composition(base)


                // draw info
                defaults()
                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = font
                drawer.text("Frame: ${counter++}", width - 150.0, height - 50.0)
            }
        }
    }
}
