import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import krangl.cumSum
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.kalasim.*
import org.kalasim.misc.cartesianProduct
import org.koin.core.Koin
import org.koin.core.component.get

// Objective: Instantiate and run a simulation multiple times in parallel

suspend fun main() {

    fun buildAtmSimulation(mu: Double, lambda: Double) = createSimulation(useCustomKoin = true) {

        val atm = Resource("atm", 1, koin =  getKoin())
        _koin.declare(atm)

        class Customer(koin:Koin) : Component(koin=koin) {
            val ed = ExponentialDistribution(rg, mu)

            override fun process() = sequence {
                yield(request(atm))

                yield(hold(ed.sample()))
                release(atm)
            }
        }

        ComponentGenerator(iat = ExponentialDistribution(rg, lambda), koin=getKoin()) {
            Customer(getKoin())
        }
    }

    // build parameter grid
    val lambdas = (1..20).map { 0.25 }.cumSum()
    val mus = (1..20).map { 0.25 }.cumSum()

    // with parallel map
    val meanAtmQueueLengthParallel = cartesianProduct(lambdas, mus).map { (lambda, mu) ->
        (lambda to mu) to lazy { buildAtmSimulation(lambda, mu) }
    }.toList().map { (params, env) ->
        env.value.run(100)
        params to env.value.get<Resource>().statistics.requesters.lengthStats.mean
    }

    println(meanAtmQueueLengthParallel.take(5))
}


// https://stackoverflow.com/questions/34697828/parallel-operations-on-kotlin-collections
suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}