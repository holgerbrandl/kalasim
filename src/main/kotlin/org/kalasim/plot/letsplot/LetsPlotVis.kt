package org.kalasim.plot.letsplot

import jetbrains.letsPlot.Stat
import jetbrains.letsPlot.geom.*
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.label.xlab
import jetbrains.letsPlot.label.ylab
import jetbrains.letsPlot.lets_plot
import krangl.asDataFrame
import krangl.toMap
import org.kalasim.monitors.FrequencyLevelMonitor
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.monitors.NumericStatisticMonitor


fun NumericLevelMonitor.display(title: String = name): Plot {
    val data: Map<String, Array<*>> = stepFun().asDataFrame().toMap()

    return lets_plot(data) + geom_step { x = "first"; y = "second" } + ggtitle(title)
}


fun NumericStatisticMonitor.display(title: String = name): Plot {
    val data: Map<String, List<Double>> = mapOf("values" to values.toList())

    return lets_plot(data) + geom_histogram { x = "values" } + ggtitle(title)
}

fun <T> FrequencyTable<T>.display(title: String? = null): Plot {
    val data: Map<String, Array<*>> = toList().asDataFrame().toMap()

    var plot = lets_plot(data) + geom_bar(stat = Stat.identity) { x = "first"; y = "second" }

    if(title != null) plot += ggtitle(title)

    return plot
}


fun <T> FrequencyLevelMonitor<T>.display(title: String = name): Plot {
    val nlmStatsData = statsData()
    val data = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: Double, val end: Double)

    val segments = data.zipWithNext().map {
        Segment(
            it.first.second,
            it.first.first,
            it.second.first
        )
    }.asDataFrame().toMap()

    // why cant we use "x".asDiscreteVariable here?
    return lets_plot(segments) + geom_segment {
        x = "start"
        y = "value"
        xend = "end"
        yend = "value"
    } + geom_point() + xlab("time") + ylab("") + ggtitle(title)
}
