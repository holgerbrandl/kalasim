package org.kalasim.test

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.kalasim.FrequencyLevelMonitor
import org.kalasim.Environment
import org.kalasim.NumericLevelMonitor
import org.kalasim.NumericStatisticMonitor
import org.kalasim.test.MonitorTests.Car.AUDI
import org.kalasim.test.MonitorTests.Car.VW
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
    fun `Frequency stats should be correct`() {
        val m = FrequencyLevelMonitor<Car>(AUDI)
//        m.addValue(AUDI)
        env.now = 2.0

        m.addValue(VW)
        env.now = 8.0

        m.getPct(AUDI) shouldBe 0.5
    }


   @Test
    fun `Frequency level stats should be correct`() {
        val m = FrequencyLevelMonitor<Car>(AUDI)
       env.now = 2.0

       m.addValue(VW)
       env.now = 8.0

       m.getPct(AUDI) shouldBe 0.25
    }


    @Test
    fun `it should correctly calculate numeric level stats`() {
        NumericLevelMonitor().apply {
            env.now += 2
            addValue(2)

            env.now += 2
            addValue(6)
            env.now += 4

//            expected value 0 -2-> 2 -2-> 6 -4-> (0+4+24)/8

            println("mean is ${mean()}")
        }.mean() shouldBe 3.5.plusOrMinus(.1)
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
