package org.kalasim.plot.letsplot

import kravis.GGPlot
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.api.util.unfold
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.facet.facetWrap
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.*
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleXDateTime
import org.kalasim.*
import org.kalasim.analysis.ResourceActivityEvent
import org.kalasim.monitors.*
import org.kalasim.plot.kravis.clistTimeline
import org.kalasim.plot.kravis.display


fun  <V:Number> MetricTimeline<V>.display(
    title: String = name,
    from: TickTime? = null,
    to: TickTime? = null,
    forceTickAxis: Boolean = false,
): Plot {
    val data = stepFun()
        .filter { from == null || it.time >= from }
        .filter { to == null || it.time <= to }

    val useWT = env.tickTransform != null && !forceTickAxis

    fun wtTransform(tt: TickTime) = if (useWT) env.toWallTime(tt) else tt.value

    return data.toDataFrame()
        .convertTick2Double("time")
        .letsPlot() +
            geomStep { x = "time"; y = "value" } + ggtitle(title)
}

private fun DataFrame<*>.convertTick2Double(colName: String): DataFrame<*> {
    return add(colName){ colName<TickTime>().value}
}


fun NumericStatisticMonitor.display(title: String = name): Plot {
    val data: Map<String, List<Double>> = mapOf("values" to values.toList())

    return letsPlot(data) + geomHistogram { x = "values" } + ggtitle(title)
}

fun <T> FrequencyTable<T>.display(title: String? = null): Plot {
    var plot = toList().toDataFrame().letsPlot() +
            geomBar(stat = Stat.identity) { x = "first"; y = "second" }

    if (title != null) plot += ggtitle(title)

    return plot
}


fun <T> CategoryTimeline<T>.display(
    title: String = name,
    forceTickAxis: Boolean = false,
): Plot {
    val nlmStatsData = statsData()
    val stepFun = nlmStatsData.stepFun()

    val useWT = env.tickTransform != null && !forceTickAxis

    fun wtTransform(tt: TickTime) = if (useWT) env.toWallTime(tt) else tt.value

    data class Segment<T>(val value: T, val start: TickTime, val end: TickTime)

    val segments = stepFun.zipWithNext().map {
        Segment(
            it.first.value,
            it.first.time,
            it.second.time
        )
    }.toDataFrame()
        .convertTick2Double("start")
        .convertTick2Double("end")
//        .addColumn("start") { expr -> expr["start"].map<Double> { wtTransform(TickTime(it)) } }
//        .addColumn("end") { expr -> expr["end"].map<Double> { wtTransform(TickTime(it)) } }
        .add("value") {  "value"<ComponentState?>()?.toString() }


    // why cant we use "x".asDiscreteVariable here?
    return segments.letsPlot() +
            geomSegment {
                x = "start"
                y = "value"
                xend = "end"
                yend = "value"
            } +
            geomPoint() +
            xlab("time") +
            ylab("") +
            ggtitle(title)
                .also { if (useWT) it + scaleXDateTime() }
}


fun List<ResourceActivityEvent>.display(
    title: String? = null,
    forceTickAxis: Boolean = false,
): Plot {
    val useWT = any { it.requestedWT != null } && !forceTickAxis

    val plotData = toDataFrame()
        //<Resource>
        .unfold<Resource>("resource", listOf("name"))
        .add("activity") { "activity"() ?: "Other" }
        .convertTick2Double("requested")
        .convertTick2Double("released")
//        .addColumn("start") { expr -> expr["start"].map<TickTime> { it.value } }
//        .addColumn("end") { expr -> expr["end"].map<TickTime> { it.value } }

    return plotData.letsPlot() +
            geomSegment(size = 10) {
                y = "name"
                yend = "name"
                x = if (useWT) "startWT" else "requested"
                xend = if (useWT) "endWT" else "released"
                color = "activity"

            } +
            ylab("") +
            xlab("Time")
                .also { if (title != null) ggtitle(title) }
                .also { if (useWT) it + scaleXDateTime() }
}


fun List<ResourceTimelineSegment>.display(
    title: String? = null,
    exclude: List<ResourceMetric> = listOf(
        ResourceMetric.Capacity,
        ResourceMetric.Occupancy,
        ResourceMetric.Availability
    ),
    forceTickAxis: Boolean = false,
): Plot {
    val useWT = any { it.startWT != null } && !forceTickAxis


    return filter { it.metric !in exclude }.toDataFrame()
//        .addColumn("start") { expr -> expr["start"].map<TickTime> { it.value } }
        .convertTick2Double("start")
        .letsPlot() +
            geomStep {

                x = if (useWT) "startWT" else "start"
                y = "value"
                color = "metric"
            } +
            // scales arg not yet supported https://github.com/JetBrains/lets-plot/issues/479
            facetWrap("color", ncol = 1)
                .also { if (title != null) ggtitle(title) }
                .also { if (useWT) it + scaleXDateTime() }
}


//
// Components
//

fun Component.display(
    title: String = stateTimeline.name,
    forceTickAxis: Boolean = false,
): GGPlot = stateTimeline.display(title = title, forceTickAxis = forceTickAxis)


fun List<Component>.displayStateTimeline(
    title: String? = null,
    componentName: String = "Component",
    forceTickAxis: Boolean = false,
): Plot {
//    val df = csTimelineDF(componentName)

    val useWT = first().env.startDate != null && !forceTickAxis
    fun wtTransform(tt: TickTime) = if (useWT) first().env.toWallTime(tt) else tt.value

    val df = clistTimeline()
        .toDataFrame()
        //<Component>
        .unfold<Component>("first", listOf("name"))
        //<LevelStateRecord<ComponentState>>
        .unfold<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))
//        .addColumn("start") { expr -> expr["timestamp"].map<Double> { wtTransform(TickTime(it)) } }
//        .addColumn("end") { expr -> (expr["timestamp"] + expr["timestamp"]).map<Double> { wtTransform(TickTime(it)) } }
        .convertTick2Double("start")
        .convertTick2Double("end")
        .add("value") { "value"<ComponentState?>()?.toString() }


    return df.letsPlot() + geomSegment {
        y = "name"
        yend = "name"
        x = "start"
        xend = "end"
        color = "value"
    } + xlab(componentName)
        .also { if (title != null) ggtitle(title) }
        .also { if (useWT) it + scaleXDateTime() }
}


fun List<Component>.displayStateProportions(
    title: String? = null,
): Plot {
    val df = clistTimeline()
        .toDataFrame()
        .unfold<Component>("first", listOf("name"))
        .unfold<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))

    return df.letsPlot() + geomBar {
        y = "name"
        fill = "value"
        weight = "duration"
    } + xlab("State Proportion")
        .also { if (title != null) ggtitle(title) }
}
