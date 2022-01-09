package org.kalasim.demo.moon.viewer

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.shape.Rectangle

fun main() = application {
    program {
        val image = loadImage("src/main/resources/1024px-Phlegra_Montes_on_Mars_ESA211127.jpg")

        val offscreen = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }
        drawer.withTarget(offscreen) {
            fill = ColorRGBa.PINK
            for(i in -5..5)
                for(j in -5..5)
                    circle(width / 2.0 + i * 30.0, height / 2.0 + j * 30.0, 15.0)
            drawer.image(image, 0.0, 0.0, width.toDouble(), height.toDouble())
        }
        val comp = compose {
            layer {
                draw {
                    drawer.image(offscreen.colorBuffer(0))
                }
            }
            layer {
                mask {
                    drawer.rectangle(
                        Rectangle.fromCenter(
                            mouse.position, 200.0, 150.0
                        )
                    )
                }
                draw {
                    drawer.image(offscreen.colorBuffer(0))
                }
                post(ApproximateGaussianBlur ().also {
                    it.window = 100
                })
            }
        }
        extend {
            comp.draw(drawer)
        }
    }
}