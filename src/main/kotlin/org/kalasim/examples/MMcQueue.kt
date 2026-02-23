@file:OptIn(AmbiguousDuration::class, AmbiguousDuration::class, AmbiguousDurationComponent::class)

package org.kalasim.examples

import org.kalasim.*
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.AmbiguousDurationComponent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Boolean
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
 *
 * @param lambda arrival rate, customers per time unit; Mean interarrival time = 1 / λ
 * @param mu service rate (per server), customers per time unit; Mean service time = 1 / μ
 */
open class MMcQueue(
    val c: Int = 1,
    val lambda: Number = 5,
    val mu: Number = 10,
    val durationUnit : DurationUnit = DurationUnit.MINUTES,
    enableInternalMetrics: Boolean = false,

) : Environment(tickDurationUnit = durationUnit) {

    // todo this should be opt-in anyway https://github.com/holgerbrandl/kalasim/issues/66
    init {
        if (!enableInternalMetrics) entityTrackingDefaults.disableAll()
    }

    val server: Resource

    val componentGenerator: ComponentGenerator<Customer>

    // disabled because not strictly needed to study the queue parameters
    //    val traces: EventLog = enableEventLog()

   inner class Customer(mu: Number, envProvider: EnvProvider= DefaultProvider()) : Component(envProvider = envProvider) {
        val ed = exponential(Rate(mu, durationUnit))

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

        val iat = exponential(Rate(lambda, durationUnit))

        componentGenerator = ComponentGenerator(iat, keepHistory = false) {
            Customer(mu, envProvider = WrappedProvider(this))
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
object MMCBig{
    @JvmStatic
    fun main(args: Array<String>) {
//        MMcQueue(c = 40, mu = 40, lambda = 800).run(10000000)
       val server= MMcQueue(c = 40, mu = 40, lambda = 800)
       server.run(1000)

//        println(server.server.occupancyTimeline.statistics())
        println("num generated "+ server.componentGenerator.numGenerated)
    }
}