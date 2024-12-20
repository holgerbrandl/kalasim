@file:OptIn(AmbiguousDuration::class, AmbiguousDuration::class, AmbiguousDurationComponent::class)

package org.kalasim.examples

import org.kalasim.*
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.AmbiguousDurationComponent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.DurationUnit

/**
 * An implementation of an MM1 queue, see https://en.wikipedia.org/wiki/M/M/1_queue, which
 * models the queue length in a system having a single server, where arrivals are
 * determined by a Poisson process and job service times have an exponential distribution. */
class MM1Queue(
    lambda: Double = 1.5,
    mu: Double = 2.0
) : MMcQueue(1, lambda, mu)


/** An implementation of an MMc queue, see https://en.wikipedia.org/wiki/M/M/c_queue, which
 * models the queue length in a system having a `c` servers, where arrivals are
 * determined by a Poisson process and job service times have an exponential distribution.
 * Also see https://commons.wikimedia.org/wiki/File:M-m-c_queue.png
 */
open class MMcQueue(
    c: Int = 1,
    lambda: Number = 5,
    mu: Number = 10,
    durationUnit : DurationUnit = DurationUnit.SECONDS
) : Environment(tickDurationUnit = durationUnit) {

    val server: Resource

    val componentGenerator: ComponentGenerator<Customer>

    // disabled because not strictly needed to study the queue parameters
    //    val traces: EventLog = enableEventLog()

    class Customer(mu: Number) : TickedComponent() {
        val ed = exponential(mu.toDouble())

        override fun process() = sequence {
            request(get<Resource>()) {
                hold(ed())
            }
        }
    }


    init {
        val rho = lambda.toDouble() / (c.toDouble() * mu.toDouble())

        println(
            "rho is ${rho}. With ρ = λ/(c*μ) >=1 the system would be unstable, " +
                    "because there are more customers then the server can serve."
        )

        server = dependency { Resource("server", c) }

        componentGenerator = ComponentGenerator(iat = exponential(lambda).minutes, keepHistory = true) {
            Customer(mu)
        }
    }
}

fun main() {
    MMcQueue(c = 3, mu = 4, lambda = 12).apply {
//        addEventListener { println(it) }

        run(1000)

        println("Average occupancy is ${server.occupancy}")

        println(SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date()))
    }
}
