import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.olive.Olive
import org.openrndr.svg.loadSVG

fun main() = application {
    configure {
        width = 768
        height = 576
    }
    program {
        extend(Olive<Program>())
    }
}


object SvgTest {
    @JvmStatic
    fun main(args: Array<String>) {
        application {
            program {
                val composition = loadSVG("src/main/resources/tractor-svgrepo-com.svg")
                extend {

                    drawer.clear(ColorRGBa.PINK)

                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.strokeWeight = 1.0

                    drawer.composition(composition)
                }
            }
        }
    }
}


object : Component("Something") {
    override fun process() = sequence {
        hold(10.minutes, description = "some action")
        // ^^ This is telling kalasim to suspend execution of this process
        // for 10 simulation minutes

        // ... 10 minutes later ...
        // After these 10 minutes, it will continue execution of the process
        hold(1.minutes, description = "some other action ")
    }
}