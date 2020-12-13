package org.kalasim.analytics

import kravis.geomHistogram
import kravis.geomStep
import kravis.plot
import kravis.render.RUtils.isInPath
import org.kalasim.NumericLevelMonitor
import org.kalasim.NumericStatisticMonitor
import java.awt.GraphicsEnvironment

internal fun canDisplay() = !GraphicsEnvironment.isHeadless() && hasR()

fun hasR(): Boolean {
    val rt = Runtime.getRuntime()
    val proc = rt.exec("R --help")
    proc.waitFor()
    return proc.exitValue() == 0
}

internal fun warnNoDisplay(): Boolean = if (canDisplay()) {
    printWarning(" No display or R not found")
    true
} else {
    false
}

internal fun printWarning(msg: String) {
    System.err.println("[kalasim] $msg")
}

internal fun NumericLevelMonitor.display() {
    if (warnNoDisplay()) return

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
    if (warnNoDisplay()) return

    apply {
        val data = values.toList()
        data.plot(
            x = { it }
        ).geomHistogram().title(name).show()
    }
}
