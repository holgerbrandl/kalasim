package org.kalasim.scratch

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics
import org.kalasim.createSimulation
import org.kalasim.monitors.*
import org.kalasim.tt

// https://www.kalasim.org/examples/
fun main() {
    createSimulation {
        val nlm = MetricTimeline()
        nlm.addValue(23)

        run(until = 10.tt)

        nlm.addValue(23)
        run(until = 12.tt)

        // has meaningful semantics
//        val mergedStats: MetricTimelineStats = listOf(nlm.statistics(),nlm.statistics()).merge()

        val nsm =
            NumericStatisticMonitor().statistics() // delegates to StatisticalSummary (so should be mergeable as well)


        val aggregate = AggregateSummaryStatistics.aggregate(listOf(nsm, nsm))


        val flm = CategoryTimeline("foo")


//        EnumeratedRealDistribution()
//        EnumeratedDistribution()


    }
}