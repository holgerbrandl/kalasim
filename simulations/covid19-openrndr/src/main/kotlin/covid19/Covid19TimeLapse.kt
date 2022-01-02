import covid19.Covid19
import covid19.PersonStatusEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.kalasim.ClockSync
import org.kalasim.Event
import org.kalasim.TickTime
import org.kalasim.tt
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import java.time.Duration


fun main() = application {
    configure {
        width = 800
        height = 700
        position = IntVector2(50, 50)
    }
    program {


        // think of it as a  Non Blocking Queue
        val ordersChannel = Channel<Event>(100)

        var now: TickTime = 0.tt

        val currentPopulation = mutableMapOf<String, PersonStatusEvent>()

        // Start a log consumer
        GlobalScope.launch {
            Covid19().apply {
                ClockSync(tickDuration = Duration.ofDays(1), syncsPerTick = 5)

                addAsyncEventListener<PersonStatusEvent> {
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
            drawer.text("NOW: ${now}", width - 100.0, height - 50.0)
            drawer.text("Frame: ${counter++}", width - 100.0, height - 70.0)
        }
    }
//    }
}


