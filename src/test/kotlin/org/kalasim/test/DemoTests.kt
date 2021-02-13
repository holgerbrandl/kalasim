package org.kalasim.test

import jetbrains.letsPlot.geom.geom_boxplot
import jetbrains.letsPlot.geom.geom_density
import jetbrains.letsPlot.geom.geom_point
import jetbrains.letsPlot.ggsize
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.lets_plot
import krangl.asDataFrame
import krangl.irisData
import krangl.print
import krangl.toMap
import org.kalasim.InteractionEvent
import org.kalasim.demo.MM1Queue
import org.kalasim.misc.showPlot
import org.kalasim.plot.letsplot.display

class DemoTests {
//TODO
}

fun main() {
    val mm1Queue = MM1Queue().apply {
        run(100)
        server.claimedMonitor.display().showPlot()
        server.requesters.lengthOfStayMonitor.display().showPlot()
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

        var p: Plot = lets_plot(data)
        p += geom_density(color = "dark_green", alpha = .3) { x = "rating"; fill = "cond" }
        p + ggsize(500, 250)
//        p.show()
        p.showPlot()


        (lets_plot(irisData.toMap()) + geom_boxplot(){ x="Species" ; y="Petal.Length" }).showPlot()
        (lets_plot(irisData.toMap()) + geom_point(){ x="Petal.Width" ; y="Petal.Length" }).showPlot()

    }
}

