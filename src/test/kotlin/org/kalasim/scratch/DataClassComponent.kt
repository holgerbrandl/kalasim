import org.kalasim.Component
import org.kalasim.analysis.ConsoleTraceLogger
import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger

// can components be extended into data classes? Yes they can.

data class Foo(val bar: String) : Component() {
}

fun main() {
    ConsoleTraceLogger.setColumnWidth(ConsoleTraceLogger.EventsTableColumn.Time, 30)

    createSimulation {
        enableComponentLogger()

        Foo("").apply {
            println(toString())
        }


    }.run(10)
}