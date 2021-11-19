package org.kalasim.plot.letsplot

import jetbrains.letsPlot.Stat
import jetbrains.letsPlot.geom.*
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.label.xlab
import jetbrains.letsPlot.label.ylab
import jetbrains.letsPlot.letsPlot
import krangl.asDataFrame
import krangl.letsPlot
import org.kalasim.monitors.CategoryTimeline
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.MetricTimeline
import org.kalasim.monitors.NumericStatisticMonitor


fun MetricTimeline.display(title: String = name): Plot {
    return stepFun().asDataFrame().letsPlot() +
            geomStep { x = "first"; y = "second" } + ggtitle(title)
}


fun NumericStatisticMonitor.display(title: String = name): Plot {
    val data: Map<String, List<Double>> = mapOf("values" to values.toList())

    return letsPlot(data) + geomHistogram { x = "values" } + ggtitle(title)
}

fun <T> FrequencyTable<T>.display(title: String? = null): Plot {
    var plot = toList().asDataFrame().letsPlot() +
            geomBar(stat = Stat.identity) { x = "first"; y = "second" }

    if (title != null) plot += ggtitle(title)

    return plot
}


fun <T> CategoryTimeline<T>.display(title: String = name): Plot {
    val nlmStatsData = statsData()
    val data = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: Double, val end: Double)

    val segments = data.zipWithNext().map {
        Segment(
            it.first.second,
            it.first.first,
            it.second.first
        )
    }.asDataFrame()

    // why cant we use "x".asDiscreteVariable here?
    return segments.letsPlot() +
            geomSegment {
                x = "start"
                y = "value"
                xend = "end"
                yend = "value"
            } +
            geomPoint() +
            xlab("time") +
            ylab("") +
            ggtitle(title)
}


