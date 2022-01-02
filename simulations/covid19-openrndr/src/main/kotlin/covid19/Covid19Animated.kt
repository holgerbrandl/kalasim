import covid19.Covid19
import covid19.PersonStatusEvent
import org.kalasim.EventLog
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgba
import org.openrndr.extra.noise.Random
import org.openrndr.math.IntVector2
import org.openrndr.math.Polar
import org.openrndr.math.Vector2

// Run a simulation and visualize the event-log in retrospect
fun main() = application {
    configure {
        width = 800
        height = 800
        position = IntVector2(50, 50)
    }
    program {
        val numSimulationDays = 30
        val covid19 = Covid19().apply {
            run(numSimulationDays)
        }

        println("simulation done")
        val log = covid19.get<EventLog>().filterIsInstance<PersonStatusEvent>()


        val grouped = log.groupBy { it.person }.mapValues { (_, values) -> values.sortedBy { it.time } }

//        for (curTime in 0 until 10000) {
        var curTime = 0

        val timeStretch = 100
        val mapScale =Math.min(width, height)/100

        extend {
            curTime = curTime.inc().rem(numSimulationDays * timeStretch)
//                drawer.clear(ColorRGBa.BLACK)

            drawer.fill = ColorRGBa.PINK
            drawer.stroke = null
//                drawer.translate(width / 2.0, height / 2.00)

            // Note: It's hard to beat that in terms of inefficiency
            val current = grouped.map { (_, log) -> log.firstOrNull() { it.time > curTime / timeStretch.toDouble() } }
                .filterNotNull()

            for (person in current) {
                drawer.fill = if (person.sick) ColorRGBa.RED else { if(person.immune) ColorRGBa.GREEN else ColorRGBa.GRAY}
                drawer.circle(Vector2(person.position.x * mapScale, person.position.y * mapScale), 7.0)
            }


            drawer.fill = ColorRGBa.WHITE
            drawer.text("NOW: ${curTime.toDouble()/timeStretch}", width -100.0, height -50.0)
        }

//        val zoom = 0.03
//
        // lighting effect, adopted from https://openrndr.org/getting-started/#getting-started-guides
//
//        drawer.fill = ColorRGBa.WHITE
//
//        extend {
//            drawer.fill = rgba(0.0, 0.0, 0.0, 0.01)
//            drawer.fill = ColorRGBa.WHITE
//            var pos = Random.point(drawer.bounds)
//            repeat(500) {
//                drawer.point(pos)
//                pos += Polar(
//                    180 * if (pos.x < width / 2)
//                        Random.value(pos * zoom)
//                    else
//                        Random.simplex(pos * zoom)
//                ).cartesian
//            }
//        }
    }
//    }
}


