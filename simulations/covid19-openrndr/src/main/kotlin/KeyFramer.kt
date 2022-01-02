import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.extra.keyframer.Keyframer
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    class Animation : Keyframer() {
        val position by Vector2Channel(arrayOf("x", "y"))
        val radius by DoubleChannel("radius")
        val color by RGBChannel(arrayOf("r", "g", "b"))
    }

    val animation = Animation()
    animation.loadFromJson(File("keyframer.json"))

    program {
        val image = loadImage("data/images/pm5544.png")
        val font = loadFont("data/fonts/default.otf", 64.0)

        extend {
            animation(seconds)
            drawer.fill = animation.color
            drawer.circle(animation.position, animation.radius)

            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("OPENRNDR", width / 2.0, height / 2.0)
        }
    }
}
