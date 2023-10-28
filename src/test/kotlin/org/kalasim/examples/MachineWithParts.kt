//MachineWithParts.kt
import org.kalasim.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class Part(val machine: Machine, partNo: Int) :
    Component(
        name = machine.name.replace("Machine", "part") + ".${partNo + 1}"
    ) {

    val ttf = uniform(19.hours, 20.hours) // time to failure distribution
    val ttr = uniform(3.hours, 6.hours)  //  time to repair distribution

    override fun process() = sequence {
        while (true) {
            hold(ttf())
            machine.interrupt()
            hold(ttr())
            machine.resume()
        }
    }
}


class Machine : Component() {

    init {
        repeat(3) { Part(this, it) }
    }

    override fun process() = sequence {
        while (true) {
            val r = get<Resource>()
            request(r)
            hold(5.minutes)
            release(r)
        }
    }
}

fun main() {
    createSimulation {
        enableComponentLogger()

        dependency { Resource() }
        repeat(2) { Machine() }

        run(400.hours)
    }
}