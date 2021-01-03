//Bank3ClerksRenegingResources.kt
package org.kalasim.examples.bank.reneging_resources


import org.kalasim.*
import org.koin.core.component.get


//var numBalked = LevelMonitoredInt(0)
var numBalked = 0
var numReneged = 0


class Customer(val clerks: Resource) : Component() {

    override fun process() = sequence {
        if (clerks.requesters.size >= 5) {
            numBalked++
            printTrace("balked")
            cancel()
        }

        request(clerks, failDelay = 50.asDist())

        if (failed) {
            numReneged++
            printTrace("reneged")
        } else {
            hold(30)
            release(clerks)
        }
    }
}

fun main() {
    declareDependencies {
        add { Resource("clerks", capacity = 3) }
    }.createSimulation {
        // register other components to  be present when starting the simulation
        ComponentGenerator(iat = uniform(5.0, 15.0, rg)) {
            Customer(get())
        }

        run(50000.0)

        val clerks = get<Resource>()

        // with console
        clerks.requesters.queueLengthMonitor.printHistogram()
        clerks.requesters.lengthOfStayMonitor.printHistogram()

        // with kravis
//        clerks.requesters.queueLengthMonitor.display()
//        clerks.requesters.lengthOfStayMonitor.display()

        println("number reneged: $numReneged")
        println("number balked: $numBalked")
    }
}
