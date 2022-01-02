import covid19.Covid19
import covid19.PersonStatusEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kalasim.ClockSync
import org.kalasim.misc.DependencyContext
import org.kalasim.seconds
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import java.time.Duration

// evolve a sim while running the visualization
fun main() = application {
    configure {
        width = 800
        height = 700
        position = IntVector2(50, 50)
    }
    program {

//        var now: TickTime = 0.tt

        val currentPopulation = mutableMapOf<String, PersonStatusEvent>()

        val covid19 = Covid19()

        // Start a log consumer
        GlobalScope.launch {
            // restore context in thread
            DependencyContext.setKoin(covid19.getKoin())

            covid19.apply {
                ClockSync(tickDuration = 1.seconds, syncsPerTick = 10)

                addEventListener<PersonStatusEvent> {
                    now = it.time
                    currentPopulation[it.person] = it
                }

                run()

                println("simulation finished prematurely")
            }
        }


        val mapScale = Math.min(width, height) / 100

        var counter = 0
        extend {
//                drawer.clear(ColorRGBa.BLACK)

            drawer.fill = ColorRGBa.PINK
            drawer.stroke = null
//                drawer.translate(width / 2.0, height / 2.00)

            for (person in currentPopulation.values.toList()) {
                drawer.fill = if (person.sick) ColorRGBa.RED else {
                    if (person.immune) ColorRGBa.GREEN else ColorRGBa.GRAY
                }
                drawer.circle(Vector2(person.position.x * mapScale, person.position.y * mapScale), 7.0)
            }


            drawer.fill = ColorRGBa.WHITE
            drawer.text("NOW: ${covid19.now}", width - 100.0, height - 50.0)
            drawer.text("Frame: ${counter++}", width - 100.0, height - 70.0)
        }
    }
//    }
}


