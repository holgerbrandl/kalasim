//Bank3ClerksRenegingResources.kt
package org.kalasim.examples.bank.reneging_resources


import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.component.get


//var numBalked = LevelMonitoredInt(0)
var numBalked = 0
var numReneged = 0


class Customer(val clerks: Resource) : Component() {

    override suspend fun SequenceScope<Component>.process(it: Component) {
        if (clerks.requesters.size >= 5) {
            numBalked++
            printTrace("balked")
            yield(cancel())
        }

        yield(request(clerks, failDelay = 50.asDist()))

        if (failed) {
            numReneged++
            printTrace("reneged")
        } else {
            yield(hold(30))
            release(clerks)
        }

    }

}

fun main() {
    val env = configureEnvironment {
        add { Resource("clerks", capacity = 3) }
    }

    env.apply {
        // register other components to  be present when starting the simulation
        ComponentGenerator(iat = UniformRealDistribution(env.rg, 5.0, 15.0)) { Customer(get()) }

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
