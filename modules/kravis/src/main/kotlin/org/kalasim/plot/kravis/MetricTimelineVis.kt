package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.geomStep
import kravis.plot
import org.kalasim.SimTime
import org.kalasim.TickTime
import org.kalasim.asSimTime
import org.kalasim.asTickTime
import org.kalasim.monitors.MetricTimeline

fun <V : Number> MetricTimeline<V>.display(
    title: String = name,
    from: TickTime,
    to: TickTime,
): GGPlot {
    return display(title, env.asSimTime(from), env.asSimTime(to), true)
}

fun <V : Number> MetricTimeline<V>.display(
    title: String = name,
    from: SimTime? = null,
    to: SimTime? = null,
    forceTickAxis: Boolean = false,
): GGPlot {
    val data = stepFun()
        .filter { from == null || it.time >= from }
        .filter { to == null || it.time <= to }

    return data
        .plot(
            x = { if(forceTickAxis) env.asTickTime(time).value else time },
            y = { value }
        )
        .xLabel("Time")
        .yLabel("")
        .geomStep()
        .title(title)
        .showOptional()
}