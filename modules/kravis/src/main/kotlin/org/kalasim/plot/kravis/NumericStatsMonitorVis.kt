package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.geomHistogram
import kravis.plot
import org.kalasim.monitors.NumericStatisticMonitor

fun NumericStatisticMonitor.display(title: String = name): GGPlot {
    val data = values.toList()

    return data.plot(x = { it })
        .geomHistogram()
        .title(title)
        .showOptional()
}