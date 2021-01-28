package org.kalasim.scratch

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.distribution.EnumeratedRealDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics
import org.kalasim.*
import org.kalasim.monitors.FrequencyLevelMonitor
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.monitors.NumericStatisticMonitor

// https://www.kalasim.org/examples/
fun main() {
    createSimulation {
        val nlm = NumericLevelMonitor()
        nlm.addValue(23)

        now=10.0

        nlm.addValue(23)
        now=12.0

        // has meaningful semantics
//        val mergedStats: NumericLevelMonitorStats = listOf(nlm.statistics(),nlm.statistics()).merge()

        val nsm = NumericStatisticMonitor().statistics() // delegates to StatisticalSummary (so should be mergeable as well)


        val aggregate = AggregateSummaryStatistics.aggregate(listOf(nsm, nsm))



        val flm = FrequencyLevelMonitor("foo")


//        EnumeratedRealDistribution()
//        EnumeratedDistribution()


    }
}