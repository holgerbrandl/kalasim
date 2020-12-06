package org.kalasim.test

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.test.MonitorTests.Car.*

//class MonitorTests : KoinStopper() {
class MonitorTests {

    private enum class Car {
        AUDI, PORSCHE, VW, TOYOTA
    }


    @Test
    fun `Frequency stats should be correct`() = createTestSimulation {
        val m = FrequencyMonitor<Car>()
        m.addValue(AUDI)
        m.addValue(AUDI)
        m.addValue(VW)
        repeat(4) { m.addValue(PORSCHE) }

        m.printHistogram()
        m.printHistogram()

        captureOutput { m.printHistogram() }.stdout shouldBe """
                Summary of: 'FrequencyMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyMonitor.1'
                              bin | entries |  pct |                                         
                AUDI              |       2 |  .29 | ***********                             
                VW                |       1 |  .14 | ******                                  
                PORSCHE           |       4 |  .57 | ***********************
                """.trimAndReline()

        captureOutput { m.printHistogram(values = listOf(AUDI, TOYOTA)) }.stdout shouldBe """
                Summary of: 'FrequencyMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyMonitor.1'
                              bin | entries |  pct |                                         
                AUDI              |       2 |  .29 | ***********                             
                TOYOTA            |       0 |  .00 |                                         
                rest              |       5 |  .71 | *****************************
                """.trimAndReline()

        captureOutput { m.printHistogram(sortByWeight = true) }.stdout shouldBe """
                Summary of: 'FrequencyMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyMonitor.1'
                              bin | entries |  pct |                                         
                PORSCHE           |       4 |  .57 | ***********************                 
                AUDI              |       2 |  .29 | ***********                             
                VW                |       1 |  .14 | ******
                """.trimAndReline()
    }


    @Test
    fun `Frequency level stats should be correct`() = createTestSimulation {
        val m = FrequencyLevelMonitor<Car>(AUDI)
//        m.addValue(AUDI)
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

        nlm.printHistogram(valueBins = false)
        nlm.printHistogram(valueBins = true)

        //TODO add test assertions here
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


internal fun createTestSimulation(builder: Environment.() -> Unit) {
    createSimulation(builder)
}
