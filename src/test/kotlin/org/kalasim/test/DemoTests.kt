package org.kalasim.test

import jetbrains.letsPlot.geom.*
import jetbrains.letsPlot.ggsize
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.letsPlot
import krangl.*
import org.kalasim.analysis.InteractionEvent
import org.kalasim.examples.MM1Queue
import org.kalasim.plot.letsplot.display


fun main() {
    val mm1Queue = MM1Queue().apply {
        run(100)
        server.claimedTimeline.display().show()
        server.requesters.lengthOfStayStatistics.display().show()
    }

    mm1Queue.traces.filterIsInstance<InteractionEvent>().asDataFrame().print()
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

