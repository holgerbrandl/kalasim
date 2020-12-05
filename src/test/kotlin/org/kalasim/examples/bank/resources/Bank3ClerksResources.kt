package org.kalasim.examples.bank.resources

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.kalasim.analytics.display
import org.koin.core.component.get


class Customer(private val clerks: Resource) : Component() {

    override fun process() = sequence {
        yield(request(clerks))
        yield(hold(30))
        release(clerks) // not really required
    }
}


fun main() {
    val env = configureEnvironment(false) {
        add { Resource("clerks", capacity = 3) }
    }.apply {
        ComponentGenerator(iat = UniformRealDistribution(rg, 5.0, 15.0)) { Customer(get()) }
    }.run(3000)

    env.get<Resource>().apply {
        printInfo()

        if (System.getenv("KALASIM_ANALYSIS") != null) {
            get<Resource>().apply {
                claimedQuantityMonitor.display()
                capacityMonitor.display()
                availableQuantityMonitor.display()
                occupancyMonitor.display()
                requesters.queueLengthMonitor.display()
                claimers.queueLengthMonitor.display()
            }
        }

        printStatistics()
    }
}

