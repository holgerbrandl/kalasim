//Bank3ClerksResources.kt
package org.kalasim.examples.bank.resources

import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


class Customer(private val clerks: Resource) : Component() {

    override fun process() = sequence {
        request(clerks)
        hold(30.minutes)
        release(clerks) // not really required
    }
}


fun main() {
    val env = createSimulation {
        dependency { Resource("clerks", capacity = 3) }

        ComponentGenerator(iat = uniform(5.0, 15.0).minutes) { Customer(get()) }
    }

    env.run(3.days)

    env.get<Resource>().apply {
        printSummary()

//        if(canDisplay()) {
//            claimedTimeline.display()
//            requesters.queueLengthTimeline.display()
//        }

        printStatistics()
    }
}

