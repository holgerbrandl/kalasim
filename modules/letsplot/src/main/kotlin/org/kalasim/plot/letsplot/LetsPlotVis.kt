package org.kalasim.plot.letsplot

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.api.util.unfoldByProperty
import org.jetbrains.kotlinx.dataframe.api.util.unfoldPropertiesByRefOf
import org.jetbrains.kotlinx.dataframe.api.util.unfoldPropertiesOf
import org.jetbrains.letsPlot.Stat
import org.jetbrains.letsPlot.facet.facetWrap
import org.jetbrains.letsPlot.geom.*
import org.jetbrains.letsPlot.intern.GenericAesMapping
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.label.*
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleXDateTime
import org.kalasim.*
import org.kalasim.analysis.ResourceActivityEvent
import org.kalasim.monitors.*

fun DataFrame<*>.letsPlot(mapping: GenericAesMapping.() -> Unit = {}) = org.jetbrains.letsPlot.letsPlot(toMap(), mapping)


fun <V : Number> MetricTimeline<V>.display(
    title: String = name,
    from: SimTime? = null,
    to: SimTime? = null,
//    forceTickAxis: Boolean = false,
): Plot {
    val data = stepFun()
        .filter { from == null || it.time >= from }
        .filter { to == null || it.time <= to }

    return data.toDataFrame()
        .convertTick2Double("time")
        .letsPlot() +
            geomStep { x = "time"; y = "value" } + ggtitle(title)
}

private fun DataFrame<*>.convertTick2Double(colName: String): DataFrame<*> {
//    return add(colName) { colName<SimTime>().epochSeconds }
    return convert { colName<SimTime>() }.with { it.epochSeconds }
}


fun NumericStatisticMonitor.display(title: String = name): Plot {
    val data: Map<String, List<Double>> = mapOf("values" to values.toList())

    return letsPlot(data) + geomHistogram { x = "values" } + ggtitle(title)
}

fun <T> FrequencyTable<T>.display(title: String? = null): Plot {
    var plot = toList().toDataFrame().letsPlot() +
            geomBar(stat = Stat.identity) { x = "first"; y = "second" }

    if(title != null) plot += ggtitle(title)

    return plot
}


fun <T> CategoryTimeline<T>.display(
    title: String = name,
    forceTickAxis: Boolean = false,
): Plot {
    val nlmStatsData = statsData()
    val stepFun = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: SimTime, val end: SimTime)

    val df = stepFun.zipWithNext().map {
        Segment(
            it.first.value,
            it.first.time,
            it.second.time
        )
    }.toDataFrame()

    println(df)

    val segments = df
        .convertTick2Double("start")
        .convertTick2Double("end")
//        .addColumn("start") { expr -> expr["start"].map<Double> { wtTransform(TickTime(it)) } }
//        .addColumn("end") { expr -> expr["end"].map<Double> { wtTransform(TickTime(it)) } }
        .convert { "value"<ComponentState?>()}.with { it?.toString() }


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
                .also { if(!forceTickAxis) it + scaleXDateTime() }
}


fun List<ResourceActivityEvent>.display(
    title: String? = null,
    forceTickAxis: Boolean = false,
): Plot {

    val plotData = toDataFrame()
        //<Resource>
        .unfoldPropertiesOf<Resource>("resource", listOf("name"))
        .add("activity") { "activity"() ?: "Other" }
        .convertTick2Double("requested")
        .convertTick2Double("released")
//        .addColumn("start") { expr -> expr["start"].map<TickTime> { it.value } }
//        .addColumn("end") { expr -> expr["end"].map<TickTime> { it.value } }

//    require(!forceTickAxis) { "tick axis not support. pls file an issue on github." }

    return plotData.letsPlot() +
            geomSegment(size = 10) {
                y = "name"
                yend = "name"
                x = if(forceTickAxis) "requestedTT" else "requested"
                xend = if(forceTickAxis) "releasedTT" else "released"
                color = "activity"

            } +
            ylab("") +
            xlab("Time")
                .also { if(title != null) ggtitle(title) }
                .also { if(!forceTickAxis) it + scaleXDateTime() }
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
    return filter { it.metric !in exclude }.toDataFrame()
//        .addColumn("start") { expr -> expr["start"].map<TickTime> { it.value } }
        .convertTick2Double("start")
        .letsPlot() +
            geomStep {

                x = if(forceTickAxis) "start" else "startTT"
                y = "value"
                color = "metric"
            } +
            // scales arg not yet supported https://github.com/JetBrains/lets-plot/issues/479
            facetWrap("color", ncol = 1)
                .also { if(title != null) ggtitle(title) }
                .also { if(!forceTickAxis) it + scaleXDateTime() }
}


//
// Components
//

internal fun List<Component>.clistTimeline() = flatMap { eqn ->
    eqn.stateTimeline
        .statsData().asList().map { eqn to it }
}

fun List<Component>.displayStateTimeline(
    title: String? = null,
    componentName: String = "Component",
    forceTickAxis: Boolean = false,
): Plot {
    val df = clistTimeline()
        .toDataFrame()
        //<Component>
        .unfoldPropertiesOf<Component>("first", listOf("name"))
        //<LevelStateRecord<ComponentState>>
        .unfoldPropertiesOf<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))
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
        .also { if(title != null) ggtitle(title) }
        .also { if(!forceTickAxis) it + scaleXDateTime() }
}


fun List<Component>.displayStateProportions(
    title: String? = null,
): Plot {
    val df = clistTimeline()
        .toDataFrame()
        .unfoldPropertiesOf<Component>("first", listOf("name"))
        .unfoldPropertiesOf<LevelStateRecord<ComponentState>>("second", listOf("timestamp", "duration", "value"))

    return df.letsPlot() + geomBar {
        y = "name"
        fill = "value"
        weight = "duration"
    } + xlab("State Proportion")
        .also { if(title != null) ggtitle(title) }
}
