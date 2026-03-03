package org.kalasim.plot.kravis

import kravis.*
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.kalasim.SimTime
import org.kalasim.State
import org.kalasim.analysis.StateChangedEvent
import org.kalasim.monitors.CategoryTimeline
import kotlin.time.DurationUnit


typealias StateChangedEventSelector<T> = StateChangedEvent<T>.(StateChangedEvent<T>) -> Any?
typealias StateChangedEventFilter<T> = (StateChangedEvent<T>) -> Boolean

fun <T> List<State<T>>.displayStateCounts(
    title: String? = null,
//    forceTickAxis: Boolean = false,
    colorBy: StateChangedEventSelector<T> = { it.newValue },
    roundTimeBy: TimeDiscretizer = TimeDiscretizer.DAY,
    filter: StateChangedEventFilter<T>? = null
): GGPlot {
    val events = flatMap { state ->
        state.timeline.getData().map { StateChangedEvent(it.first, state, it.second) }
    }.filter { sae -> filter?.let { it(sae) } ?: true }

    // lcaks initial onset (setter)
//    val events = env
//        .eventsInstanceOf<StateChangedEvent<T>>()
//        .filter { it.state in this }
//        .filter { sae -> filter?.let { it(sae) } ?: true }

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

private data class StateDurationRecord(val state: String, val duration: Double)


fun <T> List<State<T>>.displayStayDistributions(
    title: String? = null,
    from: SimTime? = null,
    to: SimTime? = null,
    durationUnit: DurationUnit = DurationUnit.MINUTES
//    colorBy: ResourceActivityEventSelector = { it.activity ?: "Other" }
): GGPlot {
    val env = first().timeline.env


    val records: List<StateDurationRecord> = flatMap { state ->
        state.timeline
            .clip(from ?: env.startDate, to ?: env.now)
            .statsData().asList()
            .filter { it.duration != null }
            .map { StateDurationRecord(it.value.toString(), it.duration!!.toDouble(durationUnit)) }
    }

    val df = records.toDataFrame()
    val plot = df.plot(
        x = "duration"
    )

    return plot
        .geomHistogram()
        .facetWrap("state")
        .xLabel("Duration (${durationUnit.toLabel()})")
        .also { if (title != null) it.title(title) }
        .showOptional()
}

/**
 * Converts a DurationUnit to a human-readable label for plotting.
 */
private fun DurationUnit.toLabel(): String = when (this) {
    DurationUnit.DAYS -> "days"
    DurationUnit.HOURS -> "hours"
    DurationUnit.MINUTES -> "minutes"
    DurationUnit.SECONDS -> "seconds"
    DurationUnit.MILLISECONDS -> "milliseconds"
    DurationUnit.MICROSECONDS -> "microseconds"
    DurationUnit.NANOSECONDS -> "nanoseconds"
}