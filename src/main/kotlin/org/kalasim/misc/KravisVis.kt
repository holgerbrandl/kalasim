package org.kalasim.misc

import kravis.*
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.FrequencyLevelMonitor
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

internal fun warnNoDisplay(): Boolean = if(!canDisplay()) {
    printWarning(" No display or R not found")
    true
} else {
    false
}

internal fun printWarning(msg: String) {
    System.err.println("[kalasim] $msg")
}

fun NumericLevelMonitor.display(title: String = name) {
    if(warnNoDisplay()) return

    apply {
        val data = stepFun()

        data.plot(
            x = Pair<Double, Double>::first,
            y = Pair<Double, Double>::second
        ).xLabel("time").yLabel("").geomStep().title(title).show()
    }
}


fun NumericStatisticMonitor.display(title: String = name) {
    if(warnNoDisplay()) return

    apply {
        val data = values.toList()
        data.plot(
            x = { it }
        ).geomHistogram().title(title).show()
    }
}

fun <T> FrequencyTable<T>.display(title: String? = null) {
    if(warnNoDisplay()) return

    val data = toList()

    data.plot(x = { it.first }, y= {it.second})
        .geomCol()
        .run { if(title != null) title(title) else this }
        .show()
}

fun <T> FrequencyLevelMonitor<T>.display(title: String = name) {
    if(warnNoDisplay()) return

    apply {
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
        segments.plot(
            x = Segment<T>::start,
            y = Segment<T>::value,
            xend = Segment<T>::end,
            yend = Segment<T>::value
        )
            .xLabel("time")
            .yLabel("")
            .geomSegment()
            .geomPoint()
            .title(title).show()
    }
}
