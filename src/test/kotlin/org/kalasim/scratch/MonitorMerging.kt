package org.kalasim.scratch

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics
import org.kalasim.createSimulation
import org.kalasim.monitors.*
import kotlin.time.Duration.Companion.minutes

// https://www.kalasim.org/examples/
fun main() {
    createSimulation {
        val nlm = MetricTimeline(initialValue = 0)
        nlm.addValue(23)

        val simStart = now
        run(until = simStart + 10.minutes)

        nlm.addValue(23)
        run(until = simStart + 12.minutes)

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