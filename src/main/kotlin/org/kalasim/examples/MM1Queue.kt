@file:OptIn(AmbiguousDuration::class)

package org.kalasim.examples

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.kalasim.*
import org.kalasim.misc.AmbiguousDuration

class MM1Queue(
    lambda: Double = 1.5,
    mu: Double = 2.0
) : Environment() {

    val server: Resource

    val componentGenerator: ComponentGenerator<Customer>

    val traces: EventLog = enableEventLog()

    class Customer(mu: Double) : Component() {
        val ed = ExponentialDistribution(env.rg, mu)

        override fun process() = sequence {
            request(get<Resource>()) {
                hold(ed())
            }
        }
    }


    init {
        val rho = lambda / mu

        println(
            "rho is ${rho}. With rho>1 the system would be unstable, " +
                    "because there are more history then the server can serve."
        )

        server = dependency { Resource("server", 1) }

        componentGenerator = ComponentGenerator(iat = exponential(lambda), keepHistory = true) {
            Customer(mu)
        }
    }
}
