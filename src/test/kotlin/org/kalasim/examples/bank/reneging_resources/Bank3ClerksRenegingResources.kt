//Bank3ClerksRenegingResources.kt
package org.kalasim.examples.bank.reneging_resources


import org.kalasim.*
import org.kalasim.monitors.printHistogram
import kotlin.time.Duration.Companion.minutes


//var numBalked = LevelMonitoredInt(0)
var numBalked = 0
var numReneged = 0


class Customer(val clerks: Resource) : Component() {

    override fun process() = sequence {
        if (clerks.requesters.size >= 5) {
            numBalked++
            log("balked")
            cancel()
        }

        request(clerks, failDelay = 50.minutes)

        if (failed) {
            numReneged++
            log("reneged")
        } else {
            hold(30.minutes)
            release(clerks)
        }
    }
}

fun main() {
    declareDependencies {
        dependency { Resource("clerks", capacity = 3) }
    }.createSimulation {
        // register other components to  be present when starting the simulation
        ComponentGenerator(iat = uniform(5.0, 15.0)) {
            Customer(get())
        }

        run(50000.minutes)

        val clerks = get<Resource>()

        // with console
        clerks.requesters.queueLengthTimeline.printHistogram()
        clerks.requesters.lengthOfStayStatistics.printHistogram()

        // with kravis
//        clerks.requesters.queueLengthMonitor.display()
//        clerks.requesters.lengthOfStayMonitor.display()

        println("number reneged: $numReneged")
        println("number balked: $numBalked")
    }
}
