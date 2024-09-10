//Bank3ClerksStandby.kt
import org.kalasim.*
import org.kalasim.plot.kravis.display
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes


class Customer(val waitingLine: ComponentQueue<Customer>) : Component() {
    override fun process() = sequence {
        waitingLine.add(this@Customer)
        passivate()
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while(true) {
            while(waitingLine.isEmpty())
                standby()

            val customer = waitingLine.poll()
            hold(32.minutes) // bearbeitungszeit
            customer.activate()
        }
    }
}


fun main() {
    val env = declareDependencies {
        dependency { ComponentQueue<Customer>("waitingline") }

    }.createSimulation {
        enableComponentLogger()

        repeat(3) { Clerk() }

        ComponentGenerator(uniform(5, 15)) { Customer(get()) }
    }

    env.run(500.minutes)

    env.get<ComponentQueue<Customer>>().apply {
        printSummary()
        println(statistics)
        lengthOfStayStatistics.display()
    }
}
