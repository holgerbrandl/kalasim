package org.kalasim.plot.kravis

import jetbrains.letsPlot.label.ggtitle
import kravis.*
import kravis.device.JupyterDevice
import org.kalasim.*
import org.kalasim.monitors.CategoryTimeline
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.MetricTimeline
import org.kalasim.monitors.NumericStatisticMonitor
import java.awt.GraphicsEnvironment

internal fun canDisplay() = !GraphicsEnvironment.isHeadless() && hasR()

fun hasR(): Boolean {
    val rt = Runtime.getRuntime()
    val proc = rt.exec("R --help")
    proc.waitFor()
    return proc.exitValue() == 0
}

internal fun checkDisplay() {
    if (!canDisplay()) {
        throw IllegalArgumentException(" No display or R not found")
    }
}

internal fun printWarning(msg: String) {
    System.err.println("[kalasim] $msg")
}

private fun GGPlot.showNotJupyter(): GGPlot = also {
    if (SessionPrefs.OUTPUT_DEVICE !is JupyterDevice) {
        checkDisplay()
        show()
    }
}


fun MetricTimeline.display(title: String = name, from: TickTime? = null, to: TickTime? = null): GGPlot {
    val data = stepFun()
        .filter { from == null || it.first >= from.value }
        .filter { to == null || it.first <= to.value }

    return data.plot(
        x = Pair<Double, Double>::first,
        y = Pair<Double, Double>::second
    ).xLabel("time")
        .yLabel("")
        .geomStep()
        .title(title)
        .showNotJupyter()
}


fun NumericStatisticMonitor.display(title: String = name): GGPlot {
    val data = values.toList()

    return data.plot(x = { it })
        .geomHistogram()
        .title(title)
        .showNotJupyter()
}

fun <T> FrequencyTable<T>.display(title: String? = null): GGPlot {
    val data = toList()

    return data.plot(x = { it.first }, y = { it.second })
        .geomCol()
        .run { if (title != null) title(title) else this }
        .showNotJupyter()
}


fun <T> CategoryTimeline<T>.display(
    title: String = name,
    forceTickAxis: Boolean = false,
): GGPlot {
    val nlmStatsData = statsData()
    val data = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: Double, val end: Double)

    val segments = data.zipWithNext().map {
        Segment(
            it.first.second,
            it.first.first,
            it.second.first
        )
    }

    // why cant we use "x".asDiscreteVariable here?
    return segments.plot(
        x = Segment<T>::start,
        y = Segment<T>::value,
        xend = Segment<T>::end,
        yend = Segment<T>::value
    )
        .xLabel("time")
        .yLabel("")
        .geomSegment()
        .geomPoint()
        .title(title)
        .showNotJupyter()
}

//
// resources
//

fun List<ResourceActivityEvent>.display(title: String?): GGPlot {
    return plot(y = { resource.name }, yend = { resource.name }, x = { start }, xend = { end }, color = { activity })
        .geomSegment(size = 10.0)
        .yLabel("Resource")
        .also { if (title != null) ggtitle(title) }
}


/**
 * @param forceTickAxis Even if a tick-transformation is defined, the x axis will show tick-times
 */
fun List<ResourceTimelineSegment>.display(
    title: String?,
    exclude: List<ResourceMetric> = listOf(
        ResourceMetric.Capacity,
        ResourceMetric.Occupancy,
        ResourceMetric.Availability
    ),
    forceTickAxis: Boolean = false,
): GGPlot {
    val useWT = any { it.startWT != null } && !forceTickAxis
    return filter { it.metric !in exclude }
        .plot(x = { if (useWT) startWT else start }, y = { value }, color = { metric })
        .geomStep()
        .facetWrap("color", ncol = 1, scales = FacetScales.free_y)
        .also { if (title != null) ggtitle(title) }
}


//
// Components

fun Component.display(
    title: String = statusTimeline.name,
    forceTickAxis: Boolean = false,
): GGPlot = statusTimeline.display(title= title, forceTickAxis= forceTickAxis)


