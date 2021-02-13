package org.kalasim.demo

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.kalasim.*

class MM1Queue(
    lambda: Double = 1.5,
    mu: Double = 2.0
) : Environment(false) {

    val server: Resource

    val traces: TraceCollector = traceCollector()

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
