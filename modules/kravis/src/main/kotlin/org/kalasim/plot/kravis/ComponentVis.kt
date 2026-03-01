package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.PositionFill
import kravis.geomBar
import kravis.geomSegment
import kravis.plot
import org.kalasim.Component
import org.kalasim.asTickTime
import org.kalasim.inMinutes
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

