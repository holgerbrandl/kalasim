package org.kalasim.test

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.test.MonitorTests.Car.AUDI
import org.kalasim.test.MonitorTests.Car.VW

//class MonitorTests : KoinStopper() {
class MonitorTests  {

    private enum class Car {
        AUDI, PORSCHE, VW
    }

    @Test
    fun `Frequency stats should be correct`() = createTestSimulation {
        val m = FrequencyLevelMonitor<Car>(AUDI)
//        m.addValue(AUDI)
        now = 2.0

        m.addValue(VW)
        now = 8.0

        m.getPct(AUDI) shouldBe 0.25
    }


    private fun createTestSimulation(builder: Environment.() -> Unit) {
        createSimulation(builder)
        Environment().apply(builder)
    }


    @Test
    fun `Frequency level stats should be correct`() = createTestSimulation {
        val m: FrequencyLevelMonitor<Car> = FrequencyLevelMonitor(AUDI)
        now = 2.0

        m.addValue(VW)
        now = 8.0

        m.getPct(AUDI) shouldBe 0.25
    }


    @Test
    fun `it should correctly calculate numeric level stats`() = createTestSimulation {
        val nlm = NumericLevelMonitor()

        now += 2
        nlm.addValue(2)

        now += 2
        nlm.addValue(6)
        now += 4

//            expected value (2*0 + 2*2 + 4*6)/8
        nlm.statistics().mean shouldBe 3.5.plusOrMinus(.1)
    }

    @Test
    fun `it should correctly calculate numeric  stats`() = createTestSimulation {
        val nsm = NumericStatisticMonitor()

        now += 2
        nsm.addValue(2)

        now += 2
        nsm.addValue(6)
        now += 4

        nsm.statistics().mean shouldBe 4.0
    }
}

typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCM(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }
