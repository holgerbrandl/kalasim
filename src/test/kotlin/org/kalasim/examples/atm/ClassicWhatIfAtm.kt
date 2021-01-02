//ClassicWhatIfAtm.kt
import krangl.*

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.kalasim.Component
import org.kalasim.ComponentGenerator
import org.kalasim.Resource
import org.kalasim.createSimulation
import org.kalasim.misc.cartesianProduct
import org.koin.core.component.get

suspend fun main() {

    fun buildAtmSimulation(mu: Double, lambda: Double) =
        createSimulation {

            val atm = Resource("atm", 1)
            _koin.declare(atm)

            class Customer : Component() {
                val ed = ExponentialDistribution(rg, mu)

                override fun process() = sequence {
                    yield(request(atm))

                    hold(ed.sample())
                    release(atm)
                }
            }

            ComponentGenerator(iat = ExponentialDistribution(rg, lambda)) {
                Customer()
            }
        }

    // build parameter grid
    val lambdas = (1..20).map { 0.25 }.cumSum()
    val mus = (1..20).map { 0.25 }.cumSum()

    val meanAtmQueueLength = cartesianProduct(lambdas, mus).map { (lambda, mu) ->
        (lambda to mu) to lazy { buildAtmSimulation(lambda, mu) }
    }.toMap().mapValues { (_, env) ->
        env.value.run(100)
        env.value.get<Resource>().statistics.requesters.lengthStats.mean
    }

    println(meanAtmQueueLength.toList().take(5))


    // or do the same more elegantly using krangl
    val df = dataFrameOf(cartesianProduct(lambdas, mus)
        .map { mapOf("lambda" to it.first, "mu" to it.second) }
        .asIterable())
        .groupByExpr { rowNumber }
        .addColumn("mean_queue_length") {
            // parameterize simulation
            val env = buildAtmSimulation(
                it["lambda"].asDoubles().first()!!,
                it["mu"].asDoubles().first()!!
            )

            env.run(100)
            env.get<Resource>().statistics.requesters.lengthStats.mean
        }

    df.head().print()
}
