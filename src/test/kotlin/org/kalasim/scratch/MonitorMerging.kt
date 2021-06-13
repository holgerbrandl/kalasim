package org.kalasim.scratch

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics
import org.kalasim.createSimulation
import org.kalasim.monitors.FrequencyLevelMonitor
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.monitors.NumericStatisticMonitor
import org.kalasim.tickTime
import org.kalasim.tt

// https://www.kalasim.org/examples/
fun main() {
    createSimulation {
        val nlm = NumericLevelMonitor()
        nlm.addValue(23)

        now = 10.tickTime

        nlm.addValue(23)
        now = 12.tickTime

        // has meaningful semantics
//        val mergedStats: NumericLevelMonitorStats = listOf(nlm.statistics(),nlm.statistics()).merge()

        val nsm =
            NumericStatisticMonitor().statistics() // delegates to StatisticalSummary (so should be mergeable as well)


        val aggregate = AggregateSummaryStatistics.aggregate(listOf(nsm, nsm))


        val flm = FrequencyLevelMonitor("foo")


//        EnumeratedRealDistribution()
//        EnumeratedDistribution()


    }
}