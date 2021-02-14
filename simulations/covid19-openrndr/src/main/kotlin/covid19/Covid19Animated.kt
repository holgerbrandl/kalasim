import covid19.Covid19
import covid19.PersonStatusEvent
import org.kalasim.TraceCollector
import org.koin.core.component.get
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2


fun main() = application {
    configure {
        width = 800
        height = 800
        position = IntVector2(50, 50)
    }
    program {
        val numSimulationDays = 10
        val covid19 = Covid19().apply {
            run(numSimulationDays)
        }

        println("simulation done")
        val log = covid19.get<TraceCollector>().filterIsInstance<PersonStatusEvent>()


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

            val current = grouped.map { (_, log) -> log.firstOrNull() { it.time > curTime / timeStretch.toDouble() } }
                .filterNotNull()

            for (person in current) {
                drawer.fill = if (person.sick) ColorRGBa.RED else ColorRGBa.GRAY
                drawer.circle(Vector2(person.position.x * mapScale, person.position.y * mapScale), 7.0)
            }


            drawer.fill = ColorRGBa.WHITE
            drawer.text("NOW: ${curTime.toDouble()/timeStretch}", width -100.0, height -50.0)
        }
    }
//    }
}


