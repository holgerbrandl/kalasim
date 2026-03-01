package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.facetWrap
import kravis.geomBar
import kravis.geomHistogram
import kravis.geomSegment
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.kalasim.SimTime
import org.kalasim.State
import org.kalasim.analysis.StateChangedEvent
import org.kalasim.eventsInstanceOf
import org.kalasim.monitors.CategoryTimeline
import kotlin.time.Duration


typealias StateChangedEventSelector<T> = StateChangedEvent<T>.(StateChangedEvent<T>) -> Any?
typealias StateChangedEventFilter<T> = (StateChangedEvent<T>) -> Boolean

fun <T> List<State<T>>.displayStateCounts(
    title: String? = null,
//    forceTickAxis: Boolean = false,
    colorBy: StateChangedEventSelector<T> = { it.newValue },
    roundTimeBy: TimeDiscretizer = TimeDiscretizer.DAY,
    filter: StateChangedEventFilter<T>? = null
): GGPlot {
    val env = first().env

    val events = env
        .eventsInstanceOf<StateChangedEvent<T>>()
        .filter { it.state in this }
        .filter { sae -> filter?.let { it(sae) } ?: true }

    val plot = events.plot(
        x = { roundTimeBy.discretize(it.time) },
        fill = colorBy
    )

    return plot
        .geomBar()
        .yLabel("")
        .xLabel("Time")
        .also { if (title != null) it.title(title) }
        .showOptional()
//        .scaleXDiscrete()
}

fun <T> List<State<T>>.displayTimelines(
    title: String? = null,
    from: SimTime? = null,
    to: SimTime? = null,
) = map { ValuedCategoryTimeline(it, it.timeline) }.displayTimelines(title, from, to)

data class ValuedCategoryTimeline<T>(val value: Any, val timeline: CategoryTimeline<T>)

fun <T> List<State<T>>.displayTimelines(
    title: String? = null,
    ySelect: StateChangedEventSelector<T> = { it.state.name },
    colorBy: ResourceActivityEventSelector = { it.activity ?: "Other" },
): GGPlot {
    val env = first().env

    return map { ValuedCategoryTimeline(it, it.timeline) }.displayTimelines()

//
//    val events = env
//        .eventsInstanceOf<StateChangedEvent<T>>()
//        .filter { it.state in this }
//
//    val timelines=map{it.timeline}
//
//    val displayTimeline = timelines.displayTimeline(title)
//
//    return displayTimeline
//
//
//
//    return events.plot(
//        y = ySelect,
//        x = { if (forceTickAxis) env.asTickTime(honored).value else honored },
//        yend = ySelect,
//        xend = { if (forceTickAxis) env.asTickTime(released).value else released },
//        color = colorBy
//    )
//        .geomSegment(size = 10.0)
//        .yLabel("")
//        .xLabel("Time")
//        .also { if (title != null) it.title(title) }
//        .showOptional()
}

fun <T> List<State<T>>.displayStayDistributions(
    title: String? = null,
    from: SimTime? = null,
    to: SimTime? = null,
//    colorBy: ResourceActivityEventSelector = { it.activity ?: "Other" }
): GGPlot {
    val env = first().timeline.env

    data class StateDurationRecord(val state: String, val duration: Int)
    val records = flatMap { state ->
        state.timeline
            .clip(from ?: env.startDate, to ?: env.now)
            .statsData().asList()
            .filter{it.duration !=null}
            .map { StateDurationRecord(it.value.toString(), it.duration!!.inWholeMinutes.toInt()) }
    }

    val df = records.toDataFrame()
    val plot = df.plot(
        x = "duration"
    )
    return plot
        .geomHistogram()
        .facetWrap("state")
        .also { if (title != null) it.title(title) }
        .showOptional()
}
