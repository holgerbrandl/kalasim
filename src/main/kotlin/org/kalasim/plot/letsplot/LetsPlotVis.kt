package org.kalasim.plot.letsplot

import jetbrains.letsPlot.Stat
import jetbrains.letsPlot.facet.facetWrap
import jetbrains.letsPlot.geom.*
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.label.xlab
import jetbrains.letsPlot.label.ylab
import jetbrains.letsPlot.letsPlot
import jetbrains.letsPlot.scale.scaleXDateTime
import krangl.*
import org.kalasim.*
import org.kalasim.monitors.*
import org.kalasim.plot.kravis.clistTimeline
import java.io.File


fun MetricTimeline.display(
    title: String = name,
    from: TickTime? = null,
    to: TickTime? = null,
    forceTickAxis: Boolean = false,
): Plot {
    val data = stepFun()
        .filter { from == null || it.first >= from.value }
        .filter { to == null || it.first <= to.value }

    val useWT = env.tickTransform != null && !forceTickAxis

    fun wtTransform(tt: TickTime) = if (useWT) env.asWallTime(tt) else tt.value

    return data.asDataFrame()
        .addColumn("first") { it["first"].map<Double> { wtTransform(TickTime(it)) } }
        .letsPlot() +
            geomStep { x = "first"; y = "second" } + ggtitle(title)
}


fun NumericStatisticMonitor.display(title: String = name): Plot {
    val data: Map<String, List<Double>> = mapOf("values" to values.toList())

    return letsPlot(data) + geomHistogram { x = "values" } + ggtitle(title)
}

fun <T> FrequencyTable<T>.display(title: String? = null): Plot {
    var plot = toList().asDataFrame().letsPlot() +
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

    fun wtTransform(tt: TickTime) = if (useWT) env.asWallTime(tt) else tt.value

    data class Segment<T>(val value: T, val start: Double, val end: Double)

    val segments = stepFun.zipWithNext().map {
        Segment(
            it.first.second,
            it.first.first,
            it.second.first
        )
    }.asDataFrame()
        .addColumn("start") { it["start"].map<Double> { wtTransform(TickTime(it)) } }
        .addColumn("end") { it["end"].map<Double> { wtTransform(TickTime(it)) } }
        .addColumn("value") { it["value"].map<ComponentState> { it.toString() } }


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
    title: String?,
    forceTickAxis: Boolean = false,
): Plot {
    val useWT = any { it.startWT != null } && !forceTickAxis

    val plotData = asDataFrame()
        .unfold<Resource>("resource", listOf("name"))
        .addColumn("activity") { it["activity"].toStrings().map { it ?: "Other" } }
        .addColumn("start") { it["start"].map<TickTime> { it.value } }
        .addColumn("end") { it["end"].map<TickTime> { it.value } }

    return plotData.letsPlot() +
            geomSegment(size = 10) {
                y = "name"
                yend = "name"
                x = if (useWT) "startWT" else "start"
                xend = if (useWT) "endWT" else "end"
                color = "activity"

            } +
            ylab("") +
            xlab("Time")
                .also { if (title != null) ggtitle(title) }
                .also { if (useWT) it + scaleXDateTime() }
}


fun List<ResourceTimelineSegment>.display(
    title: String?,
    exclude: List<ResourceMetric> = listOf(
        ResourceMetric.Capacity,
        ResourceMetric.Occupancy,
        ResourceMetric.Availability
    ),
    forceTickAxis: Boolean = false,
): Plot {
    val useWT = any { it.startWT != null } && !forceTickAxis


    return filter { it.metric !in exclude }.asDataFrame()
        .addColumn("start") { it["start"].map<TickTime> { it.value } }
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
    title: String = statusTimeline.name,
    forceTickAxis: Boolean = false,
): Plot = statusTimeline.display(title = title, forceTickAxis = forceTickAxis)


fun List<Component>.displayStateTimeline(
    title: String? = null,
    componentName: String = "Component",
    forceTickAxis: Boolean = false,
): Plot {
//    val df = csTimelineDF(componentName)

    val useWT = first().tickTransform != null && !forceTickAxis
    fun wtTransform(tt: TickTime) = if (useWT) first().env.asWallTime(tt) else tt.value

    val df = clistTimeline()
        .asDataFrame()
        .unfold<Component>("first", listOf("name"))
        .unfold<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))
        .addColumn("start") { it["timestamp"].map<Double> { wtTransform(TickTime(it)) } }
        .addColumn("end") { (it["timestamp"] + it["timestamp"]).map<Double> { wtTransform(TickTime(it)) } }
        .addColumn("value") { it["value"].map<ComponentState> { it.toString() } }


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
        .asDataFrame()
        .unfold<Component>("first", listOf("name"))
        .unfold<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))

    return df.letsPlot() + geomBar() {
        y = "name"
        fill = "value"
        weight = "duration"
    } + xlab("State Proportion")
        .also { if (title != null) ggtitle(title) }
}
