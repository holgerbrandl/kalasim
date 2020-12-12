package org.kalasim.analytics

import kravis.geomHistogram
import kravis.geomStep
import kravis.plot
import org.kalasim.NumericLevelMonitor
import org.kalasim.NumericStatisticMonitor

internal fun NumericLevelMonitor.display() {
    apply {
        val nlmStatsData = valuesUntilNow()

        val data = nlmStatsData.plotData()

        data.plot(
            x = Pair<Double, Double>::first,
            y = Pair<Double, Double>::second
        ).xLabel("time").yLabel("").geomStep().title(name).show()
    }
}


internal fun NumericStatisticMonitor.display() {
    apply {
        val data = values.toList()
        data.plot(
            x = { it }
        ).geomHistogram().title(name).show()
    }
}
