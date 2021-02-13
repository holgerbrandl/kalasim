package org.kalasim.demo

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.kalasim.*

class MM1Queue(
    val lambda: Double = 1.5,
    val mu: Double = 1.0
) : Environment(true) {

    val server: Resource

    init {
        val rho = lambda / mu

        println(
            "rho is ${rho}. With rho>1 the system would be unstable, " +
                    "because there are more arrivals then the server can serve."
        )

        server = Resource("server", 1)

        class Customer : Component() {
            val ed = ExponentialDistribution(rg, mu)

            override fun process() = sequence {
                request(server) {
                    hold(ed.sample())
                }
            }
        }

        ComponentGenerator(iat = exponential(lambda)) {
            Customer()
        }
    }
}
