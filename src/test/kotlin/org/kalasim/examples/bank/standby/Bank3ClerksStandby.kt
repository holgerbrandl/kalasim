//Bank3ClerksStandby.kt
import org.kalasim.*
import org.kalasim.analytics.display
import org.koin.core.component.get
import org.koin.core.component.inject


class Customer(val waitingLine: ComponentQueue<Customer>) : Component() {
    override fun process() = sequence{
        waitingLine.add(this@Customer)
        passivate()
    }
}


class Clerk : Component() {
    val waitingLine: ComponentQueue<Customer> by inject()

    override fun process() = sequence {
        while (true) {
            while(waitingLine.isEmpty())
                standby()

            val customer = waitingLine.poll()
            hold(32.0) // bearbeitungszeit
            customer.activate()
        }
    }
}


fun main() {
    val env = declareDependencies {
        add { ComponentQueue<Customer>("waitingline") }

    }.createSimulation(true) {
        repeat(3) { Clerk() }

        ComponentGenerator(uniform(5,15)){ Customer(get()) }

    }

    env.run(500.0)

    env.get<ComponentQueue<Customer>>().apply {
        printInfo()
        printStats()
        lengthOfStayMonitor.display()
    }
}
