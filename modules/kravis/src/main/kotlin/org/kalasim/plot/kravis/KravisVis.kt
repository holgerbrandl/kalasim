package org.kalasim.plot.kravis

import kravis.*
import kravis.device.JupyterDevice
import org.kalasim.*
import org.kalasim.Component
import org.kalasim.analysis.ResourceActivityEvent
import org.kalasim.monitors.*
import java.awt.*
import kotlin.time.Duration

fun canDisplay() = !GraphicsEnvironment.isHeadless() && hasR()

fun hasR(): Boolean {
    try {
        val rt = Runtime.getRuntime()
        val proc = rt.exec("R --help")
        proc.waitFor()
        return proc.exitValue() == 0
    } catch(e: Throwable) {
        return false
    }
}

internal fun checkDisplay() {
    if(!canDisplay()) {
        throw IllegalArgumentException(" No display or R not found")
    }
}

internal fun printWarning(msg: String) {
    System.err.println("[kalasim] $msg")
}

private fun GGPlot.showOptional(): GGPlot = also {
    if(USE_KRAVIS_VIEWER && SessionPrefs.OUTPUT_DEVICE !is JupyterDevice) {
        checkDisplay()
        show()
    }
}

var USE_KRAVIS_VIEWER = false

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


fun NumericStatisticMonitor.display(title: String = name): GGPlot {
    val data = values.toList()

    return data.plot(x = { it })
        .geomHistogram()
        .title(title)
        .showOptional()
}

fun <T> FrequencyTable<T>.display(title: String? = null): GGPlot {
    val data = toList()

    return data.plot(x = { it.first }, y = { it.second })
        .geomCol()
        .run { if(title != null) title(title) else this }
        .showOptional()
}


fun <T> CategoryTimeline<T>.display(
    title: String = name,
    forceTickAxis: Boolean = false,
): GGPlot {
    val nlmStatsData = statsData()
    val stepFun = nlmStatsData.stepFun()

    data class Segment<T>(val value: T, val start: SimTime, val end: SimTime)

    val segments = stepFun.zipWithNext().map {
        Segment(
            it.first.value,
            it.first.time,
            it.second.time
        )
    }

    // why cant we use "x".asDiscreteVariable here?
    return segments.plot(
        x = { if(forceTickAxis) env.asTickTime(start).value else start },
        y = { value },
        xend = { if(forceTickAxis) env.asTickTime(end).value else end },
        yend = { value }
    )
        .xLabel("Time")
        .yLabel("")
        .geomSegment()
        .geomPoint()
        .title(title)
        .showOptional()
}

//
// resources
//

fun List<ResourceActivityEvent>.display(
    title: String? = null,
    forceTickAxis: Boolean = false,
): GGPlot {
    val env = this@display.first().requester.env

    return plot(y = { resource.name },
        x = { if(forceTickAxis) env.asTickTime(honored).value else honored },
        yend = { resource.name },
        xend = { if(forceTickAxis) env.asTickTime(released).value else released },
        color = { activity ?: "Other" })
        .geomSegment(size = 10.0)
        .yLabel("")
        .xLabel("Time")
        .also { if(title != null) it.title(title) }
        .showOptional()
}


/**
 * @param forceTickAxis Even if a tick-transformation is defined, the x axis will show tick-times
 */
fun List<ResourceTimelineSegment>.display(
    title: String? = null,
    exclude: List<ResourceMetric> = listOf(
        ResourceMetric.Capacity,
        ResourceMetric.Occupancy,
        ResourceMetric.Availability
    ),
    forceTickAxis: Boolean = false,
): GGPlot {
//    val useWT = any { it.startWT != null } && !forceTickAxis
    val env = this@display.first().resource.env

    return filter { it.metric !in exclude }
        .plot(x = { if(forceTickAxis) env.asTickTime(start).value else start }, y = { value }, color = { metric })
        .geomStep()
        .facetWrap("color", ncol = 1, scales = FacetScales.free_y)
        .also { if(title != null) it.title(title) }
        .showOptional()
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
): GGPlot {
//    val df = csTimelineDF(componentName)
    val df = clistTimeline()

    val env = first().env

    return df.plot(
        y = { first.name },
        yend = { first.name },
        x = { with(second) { if(forceTickAxis) env.asTickTime(timestamp).value else timestamp } },
        xend = {
            with(second.timestamp + (second.duration ?: Duration.ZERO)) {
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

//private fun List<Component>.csTimelineDF(componentName: String) = map { eqn ->
//    eqn.statusTimeline
//        .statsData().asList()
//        .toDataFrame().addColumn(componentName) { eqn }
//}.concat().rename("value" to "state")


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
        .also { if(title != null) it.title(title) }
        .showOptional()
}


// not needed because displayStateProportions works so much better here
//fun List<Component>.displayStateHeatmap(
//    title: String? = null,
//    componentName: String = "component",
//    forceTickAxis: Boolean = false,
//): GGPlot {
//    val df = clistTimeline()
//
//    val dfUnfold = df.toDataFrame().unfold<LevelStateRecord<ComponentState>>("second").rename("first" to "component")
//
//    val durationSummary = dfUnfold
//        .groupBy("state", "component")
//        .summarize("total_duration" `=` { it["duration"] })
//
//    val propMatrix = durationSummary
//        .groupBy("component")
//        .addColumn("total_dur_prop"){ it["total_duration"]/ it["total_duration"].sum()!! }
//
//    return propMatrix.plot("component", y="state", fill="total_dur_prop")
//        .geomTile()
//        .also { if (title != null) ggtitle(title) }
//}

internal fun List<Component>.clistTimeline() = flatMap { eqn ->
    eqn.stateTimeline
        .statsData().asList().map { eqn to it }
}

