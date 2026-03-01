package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.geomPoint
import kravis.geomSegment
import kravis.plot
import org.kalasim.SimTime
import org.kalasim.asTickTime
import org.kalasim.monitors.CategoryTimeline
import kotlin.time.Duration

@Deprecated("use displayTimeline", ReplaceWith("displayTimeline(title)"))
fun <T> CategoryTimeline<T>.display(title: String = name) = displayTimelines(name)

fun <T> CategoryTimeline<T>.displayTimelines(
    title: String = name,
    forceTickAxis: Boolean = false,
): GGPlot {
    val nlmStatsData = statsData()
    val stepFun = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: SimTime, val end: SimTime)

    val segments = stepFun.zipWithNext().map {
        Segment(it.first.value, it.first.time, it.second.time)
    }

    // why can't we use "x".asDiscreteVariable here?
    return segments.plot(
        x = { if (forceTickAxis) env.asTickTime(start).value else start },
        y = { value },
        xend = { if (forceTickAxis) env.asTickTime(end).value else end },
        yend = { value })
        .xLabel("Time")
        .yLabel("")
        .geomSegment()
        .geomPoint()
        .title(title)
        .showOptional()
}


fun <T> List<CategoryTimeline<T>>.displayTimelines() = map { ValuedCategoryTimeline(it.name, it) }.displayTimelines()


fun <T> List<ValuedCategoryTimeline<T>>.displayTimelines(
    title: String? = null,
    from: SimTime? = null,
    to: SimTime? = null,
//    colorBy: ResourceActivityEventSelector = { it.activity ?: "Other" }
): GGPlot {
    val env = first().timeline.env

    val records = flatMap { ctl ->
        ctl.timeline
            .clip(from ?: env.startDate, to ?: env.now)
            .statsData().asList()
            .map { ctl.timeline to it }
    }

//    data class TimelineEvent(val name: String, val fixedEnd: SimTime, val value: T)
//    return listOf<TimelineEvent>().plot(
    val plot = records.plot(
        y = { first },
        x = { second.timestamp },
        yend = { first },
        xend = { second.timestamp + (second.duration ?: Duration.ZERO) },
        color = { second.value }
    )
    return plot
        .geomSegment(size = 10.0)
        .yLabel("")
        .xLabel("Time")
        .also { if (title != null) it.title(title) }
        .showOptional()
}
