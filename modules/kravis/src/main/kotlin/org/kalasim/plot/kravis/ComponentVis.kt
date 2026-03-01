package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.geomSegment
import kravis.plot
import org.kalasim.Component
import org.kalasim.asTickTime
import kotlin.time.Duration

fun Component.display(
    title: String = stateTimeline.name,
    forceTickAxis: Boolean = false,
): GGPlot = stateTimeline.displayTimelines(title = title, forceTickAxis = forceTickAxis)


fun List<Component>.displayStateTimeline(
    title: String? = null,
    componentName: String = "Component",
    forceTickAxis: Boolean = false,
): GGPlot {
//    val df = csTimelineDF(componentName)
    val df = clistTimeline()

    val env = first().env

    return df.plot(
        y = { first.name },
        yend = { first.name },
        x = { with(second) { if(forceTickAxis) env.asTickTime(timestamp).value else timestamp } },
        xend = {
            with(second.timestamp + (second.duration ?: Duration.Companion.ZERO)) {
                if(forceTickAxis) env.asTickTime(this).value else this
            }
        },
        color = { second.value }
    )
        .geomSegment()
        .also { if(title != null) it.title(title) }
        .xLabel(componentName)
        .showOptional()
}