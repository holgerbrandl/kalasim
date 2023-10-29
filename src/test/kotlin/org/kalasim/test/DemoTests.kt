package org.kalasim.test

import krangl.irisData
import krangl.toMap
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.letsPlot
import org.kalasim.EventLog
import org.kalasim.analysis.InteractionEvent
import org.kalasim.enableEventLog
import org.kalasim.examples.MM1Queue
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.plot.letsplot.display


@OptIn(AmbiguousDuration::class)
fun main() {
    val mm1Queue = MM1Queue().apply {
        enableEventLog()

        run(100)
        server.claimedTimeline.display().show()
        server.requesters.lengthOfStayStatistics.display().show()
    }

    mm1Queue.get<EventLog>().filterIsInstance<InteractionEvent>().toDataFrame().print()
}


object LetsPlotASimulation {
    @JvmStatic
    fun main(args: Array<String>) {
        val rand = java.util.Random()
        val data = mapOf<String, Any>(
            "rating" to List(200) { rand.nextGaussian() } + List(200) { rand.nextGaussian() * 1.5 + 1.5 },
            "cond" to List(200) { "A" } + List(200) { "B" }
        )

        var p: Plot = letsPlot(data)
        p += geomDensity(color = "dark_green", alpha = .3) { x = "rating"; fill = "cond" }
        p + ggsize(500, 250)
//        p.show()
        p.show()


        (letsPlot(irisData.toMap()) + geomBoxplot { x = "Species"; y = "Petal.Length" }).show()
        (letsPlot(irisData.toMap()) + geomPoint { x = "Petal.Width"; y = "Petal.Length" }).show()

    }
}

