package org.kalasim.plot.kravis

import kravis.*
import kravis.device.JupyterDevice
import org.kalasim.monitors.FrequencyLevelMonitor
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.NumericLevelMonitor
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
    if(!canDisplay()) {
        throw IllegalArgumentException(" No display or R not found")
    }
}

internal fun printWarning(msg: String) {
    System.err.println("[kalasim] $msg")
}


private fun GGPlot.showNotJupyter(): GGPlot = also {
    if(SessionPrefs.OUTPUT_DEVICE !is JupyterDevice) show()
}


fun NumericLevelMonitor.display(title: String = name): GGPlot {
    checkDisplay()

    val data = stepFun()

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
    checkDisplay()

    val data = values.toList()

    return data.plot(x = { it })
        .geomHistogram()
        .title(title)
        .showNotJupyter()
}

fun <T> FrequencyTable<T>.display(title: String? = null): GGPlot {
    checkDisplay()

    val data = toList()

    return data.plot(x = { it.first }, y = { it.second })
        .geomCol()
        .run { if(title != null) title(title) else this }
        .showNotJupyter()
}


fun <T> FrequencyLevelMonitor<T>.display(title: String = name): GGPlot {
    checkDisplay()

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
