package org.kalasim.examples.bank.resources

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.koin.core.get

class Customer(val clerks: Resource) : Component() {

    override fun process() = sequence {
        yield(request(clerks))
        yield(hold(30.0))
        release(clerks) // not really required
    }
}


fun main() {
    val env = configureEnvironment(false) {
        add { Resource("clerks", capacity = 3.0) }
    }.apply {
        ComponentGenerator(iat = UniformRealDistribution(rg, 5.0, 15.0)) { Customer(get()) }
    }.run(50000.0)

    env.get<Resource>().apply{
        printInfo()
        printStatistics()
    }
}

