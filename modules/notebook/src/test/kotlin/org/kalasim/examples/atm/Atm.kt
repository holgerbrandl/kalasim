//Atm.kt


import kotlinx.coroutines.*
import kravis.geomTile
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.rename
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.api.util.unfoldPropertiesOf
import org.kalasim.*
import org.kalasim.misc.*
import org.kalasim.plot.kravis.display
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days

//https://youtrack.jetbrains.com/issue/KT-44062

fun main() {

// simples simulation
    createSimulation {

        val lambda = 1.5
        val mu = 1.0
        val rho = lambda / mu

        println(
            "rho is ${rho}. With rho>1 the system would be unstable, " +
                    "because there are more history then the atm can serve."
        )

        val atm = Resource("atm", 1)

        class Customer : Component() {
            val ed = exponential(mu).minutes

            override fun process() = sequence {

                request(atm)

                hold(ed.sample())
                release(atm)
            }
        }

        ComponentGenerator(iat = exponential(lambda).minutes) { Customer() }

        run(1.week)

        atm.occupancyTimeline.display()
        atm.requesters.queueLengthTimeline.display()
        atm.requesters.lengthOfStayStatistics.display()

        println(atm.requesters.lengthOfStayStatistics.statistics())
    }
}

// Classical WhatIf
// Define simulation entities
@Suppress("CanBeParameter")
class AtmCustomer(
    val mu: Double,
    val atm: Resource,
) : Component() {
    val ed = exponential(mu).minutes

    override fun process() = sequence {
        request(atm) {
            hold(ed.sample())
        }
    }
}

class AtmQueue(val lambda: Double, val mu: Double) : Environment() {
    val atm = dependency { Resource("atm", 1) }

    init {
        ComponentGenerator(iat = exponential(lambda).minutes) {
            AtmCustomer(mu, atm)
        }
    }
}

object WhatIf {

    @JvmStatic

    fun main(args: Array<String>) {
        // build parameter grid
        val lambdas = (1..20).map { 0.25 }.cumSum()
        val mus = (1..20).map { 0.25 }.cumSum()

        // run 100x times
        val atms = cartesianProduct(lambdas, mus).map { (lambda, mu) ->
            AtmQueue(lambda, mu).apply { run(10.days) }
        }

        atms.map {
            it to it.get<Resource>().statistics.requesters.lengthStats.mean!!.roundAny(2)
        }.toList()
            .toDataFrame()
            .unfoldPropertiesOf<AtmQueue>("first", listOf("rho", "lambda"))
            .rename("second" to "mean_queue_length")
    }
}

@OptIn(DelicateCoroutinesApi::class)
object PWhatIf {
    @JvmStatic
    fun main(args: Array<String>) {
        val lambdas = (1..20).map { 0.25 }.cumSum()
        val mus = (1..20).map { 0.25 }.cumSum()


        val atms = cartesianProduct(lambdas, mus).asIterable().map { (lambda, mu) ->
            // instantiate sequentially to simplify dependency injection
            AtmQueue(lambda, mu)
        }.toList()

        println(atms)
        // define parallelization helper to run in parallel
        // https://stackoverflow.com/questions/34697828/parallel-operations|-on-kotlin-collections
        fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = runBlocking {
            map { async(newFixedThreadPoolContext(4, "")) { f(it) } }.map { it.await() }
        }

        // simulate in parallel
        atms.pmap {
            it.run(12.hours)
        }

        // extract stats and visualize
        val meanQLength = atms.map { it to it.get<Resource>().statistics.requesters.lengthStats.mean!! }

        meanQLength.plot(x = { first.lambda }, y = { first.mu }, fill = { second })
            .geomTile()
            .title("Mean ATM Queue Length vs Lambda and Mu")
            .xLabel("Lambda").yLabel("Mu")
    }
}