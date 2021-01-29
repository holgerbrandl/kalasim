package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues
import org.junit.Test
import org.kalasim.misc.merge
import org.kalasim.misc.mergeStats
import org.kalasim.monitors.*
import org.kalasim.test.MonitorTests.Car.*

class MonitorTests {

    private enum class Car {
        AUDI, PORSCHE, VW, TOYOTA
    }


    @Test
    fun `Frequency stats should be correct`() = createTestSimulation {
        val m = FrequencyStatsMonitor<Car>()
        m.addValue(AUDI)
        m.addValue(AUDI)
        m.addValue(VW)
        repeat(4) { m.addValue(PORSCHE) }

        m.printHistogram()

        captureOutput { m.printHistogram() }.stdout shouldBe """
                Summary of: 'FrequencyStatsMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyStatsMonitor.1'
                              bin |  values |  pct |                                         
                AUDI              |    2.00 |  .29 | ***********                             
                VW                |    1.00 |  .14 | ******                                  
                PORSCHE           |    4.00 |  .57 | ***********************
                                """.trimIndent()

        captureOutput { m.printHistogram(values = listOf(AUDI, TOYOTA)) }.stdout shouldBe """
                Summary of: 'FrequencyStatsMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyStatsMonitor.1'
                              bin |  values |  pct |                                         
                AUDI              |    2.00 |  .29 | ***********                             
                TOYOTA            |     .00 |  .00 |                                         
                rest              |    5.00 |  .71 | *****************************
                """.trimIndent()

        captureOutput { m.printHistogram(sortByWeight = true) }.stdout shouldBe """
                Summary of: 'FrequencyStatsMonitor.1'
                # Records: 7
                # Levels: 3
                
                Histogram of: 'FrequencyStatsMonitor.1'
                              bin |  values |  pct |                                         
                PORSCHE           |    4.00 |  .57 | ***********************                 
                AUDI              |    2.00 |  .29 | ***********                             
                VW                |    1.00 |  .14 | ******
                """.trimIndent()
    }


    @Test
    fun `Frequency level stats should be correct`() = createTestSimulation {
        val m = FrequencyLevelMonitor<Car>(AUDI)
//        m.addValue(AUDI)
        now = 2.0

        m.addValue(VW)
        now = 8.0

        m.getPct(AUDI) shouldBe 0.25

        // todo add test assertions
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

    @Test
    fun `disabled monitor should error nicely when being queried`() = createTestSimulation {
        //FrequencyMonitor
        run {
            val nsm = FrequencyStatsMonitor<Int>()

            nsm.addValue(2)
            nsm.disable()

            shouldThrow<IllegalArgumentException> {
                nsm.statistics.size
            }

            // should be silently ignores
            nsm.addValue(2)

            shouldThrow<IllegalArgumentException> {
                nsm.printHistogram()
            }
        }
    }

    // TODO test the others
}

/* Test the monitors and their stats can be merge jor a joint analysis. */
class MergeMonitorTests {

    @Test
    fun `it should merge NLM`() = createTestSimulation {
        val nlmA = NumericLevelMonitor()
        val nlmB = NumericLevelMonitor()

        now = 5.0
        nlmA.addValue(23)

        now = 10.0
        nlmB.addValue(3)

        now = 12.0
        nlmB.addValue(5)

        now = 14.0
        nlmA.addValue(10)

        //merge statistics
        val mergedStats: EnumeratedDistribution<Number> = listOf(nlmA, nlmB).mergeStats()
        println(mergedStats)
    }


    @Test
    fun `it should merge FLM`() = createTestSimulation {
        val flmA = FrequencyLevelMonitor(1)
        val flmB = FrequencyLevelMonitor(2)

        flmA.addValue(1)
        flmB.addValue(2)

        now = 1.0
        flmB.addValue(4)

        now = 3.0
        flmA.addValue(1)

        //merge statistics
        now = 5.0
        val mergedStats: EnumeratedDistribution<Int> = listOf(flmA, flmB).mergeStats()
        println(mergedStats)
    }


    @Test
    fun `it should merge NSM`() = createTestSimulation {
        // note NSM.statistics() // delegates to StatisticalSummary (so should be mergeable as well)
        val nsmA = NumericStatisticMonitor()
        val nsmB = NumericStatisticMonitor() // delegates to StatisticalSummary (so should be mergeable as well)

        nsmA.apply {
            addValue(1)
            addValue(1)
        }

        nsmB.apply {
            addValue(3)
            addValue(3)
        }

        val mergedStats: StatisticalSummaryValues = listOf(nsmA.statistics(), nsmB.statistics()).merge()
        println(mergedStats)
        mergedStats.mean shouldBe 2
    }


    @Test
    fun `it should merge FSM`() = createTestSimulation {
        val fsmA = FrequencyStatsMonitor<Int>()
        val fsmB = FrequencyStatsMonitor<Int>()

        fsmA.apply {
            addValue(1)
            addValue(1)
        }

        fsmB.apply {
            addValue(3)
            addValue(3)
        }

        print(fsmA.info)

        val mergedDist: FrequencyTable<Int> = listOf(fsmA, fsmB).mergeStats()

        //todo test this
        print(mergedDist)
    }

}