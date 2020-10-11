package org.github.holgerbrandl.kalasim.test

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.github.holgerbrandl.kalasim.DiscreteLevelMonitor
import org.github.holgerbrandl.kalasim.Environment
import org.github.holgerbrandl.kalasim.NumericLevelMonitor
import org.github.holgerbrandl.kalasim.NumericStatisticMonitor
import org.github.holgerbrandl.kalasim.test.MonitorTests.Car.AUDI
import org.github.holgerbrandl.kalasim.test.MonitorTests.Car.VW
import org.junit.BeforeClass
import org.junit.Test

class MonitorTests {

    companion object {
        private lateinit var env: Environment

        @BeforeClass
        @JvmStatic
        fun setup() {
            env = Environment()
        }
    }

    private enum class Car {
        AUDI, PORSCHE, VW
    }

    @Test
    fun `level means should be correct`() {
        val m = DiscreteLevelMonitor<Car>()

        env.now = 10.0
        m.addValue(AUDI)
        env.now = 22.0

        m.addValue(VW)

//        m.addValue()

    }

    // Adopted from example in https://www.salabim.org/manual/Monitor.html
    @Test
    fun `it should print a histogram for discrete level stats`() {
        val data = listOf<Pair<String, Double>>(
            "foo" to 0.1,
            "bar" to 0.9
        )

        val dist = EnumeratedDistribution(data.asCM())

        val dm = DiscreteLevelMonitor<String>()

        repeat(1000) { dm.addValue(dist.sample()); env.now += 1 }

        dm.printStats()

    }

    @Test
    fun `it should correctly calculate numeric level stats`() {
        NumericLevelMonitor().apply {
            env.now += 2
            addValue(2)

            env.now += 2
            addValue(6)
            env.now += 4

            println("mean is ${mean()}")
        }.mean() shouldBe 4.6.plusOrMinus(.1)
    }

    @Test
    fun `it should correctly calculate numeric  stats`() {
        NumericStatisticMonitor().apply {
            env.now += 2
            addValue(2)

            env.now += 2
            addValue(6)
            env.now += 4

            println("mean is ${mean()}")
        }.mean() shouldBe 4.0
    }
}

typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCM(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }
