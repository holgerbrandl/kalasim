package org.kalasim.plot.kravis

import kravis.*
import kravis.device.JupyterDevice
import org.kalasim.Component
import org.kalasim.inMinutes
import java.awt.GraphicsEnvironment

fun canDisplay() = !GraphicsEnvironment.isHeadless() && hasR()

fun hasR(): Boolean {
    try {
        val rt = Runtime.getRuntime()
        val proc = rt.exec("R --help")
        proc.waitFor()
        return proc.exitValue() == 0
    } catch (e: Throwable) {
        return false
    }
}

internal fun checkDisplay() {
    if (!canDisplay()) {
        throw IllegalArgumentException(" No display or R not found")
    }
}

//internal fun printWarning(msg: String) {
//    System.err.println("[kalasim] $msg")
//}

internal fun GGPlot.showOptional(): GGPlot = also {
    if (USE_KRAVIS_VIEWER && SessionPrefs.OUTPUT_DEVICE !is JupyterDevice) {
        checkDisplay()
        show()
    }
}


var USE_KRAVIS_VIEWER = false


//
// Components
//


fun List<Component>.displayStateProportions(
    title: String? = null,
): GGPlot {
    val df = clistTimeline()


    return df.plot(
        y = { first.name },
        fill = { second.value },
        weight = { second.duration?.inMinutes }
    )
        .geomBar(position = PositionFill())
        .xLabel("State Proportion")
        .also { if (title != null) it.title(title) }
        .showOptional()
}


internal fun List<Component>.clistTimeline() = flatMap { eqn ->
    eqn.stateTimeline
        .statsData().asList().map { eqn to it }
}

