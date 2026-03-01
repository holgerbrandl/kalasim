package org.kalasim.plot.kravis


import kravis.FacetScales
import kravis.GGPlot
import kravis.facetWrap
import kravis.geomSegment
import kravis.geomStep
import kravis.plot
import org.kalasim.Resource
import org.kalasim.ResourceMetric
import org.kalasim.ResourceTimelineSegment
import org.kalasim.analysis.ResourceActivityEvent
import org.kalasim.asTickTime
import org.kalasim.eventsInstanceOf

fun List<Resource>.foo2(){}

typealias ResourceActivityEventSelector = ResourceActivityEvent.(ResourceActivityEvent) -> Any?

//@JvmName("displayResourceTimeline")
fun List<Resource>.displayTimelines(
    title: String? = null,
    byRequester: Boolean = false,
    forceTickAxis: Boolean = false,
    colorBy: ResourceActivityEventSelector={ it.activity?: "Other"}
): GGPlot {
    val env = first().env
    val events = env.eventsInstanceOf<ResourceActivityEvent>().filter { contains(it.resource) }


    val ySelect: ResourceActivityEventSelector = { if(byRequester) resource.name else requester.name }

    return events.plot(y = ySelect,
        x = { if(forceTickAxis) env.asTickTime(honored).value else honored },
        yend = ySelect,
        xend = { if(forceTickAxis) env.asTickTime(released).value else released },
        color = colorBy)
        .geomSegment(size = 10.0)
        .yLabel("")
        .xLabel("Time")
        .also { if(title != null) it.title(title) }
        .showOptional()
}


/**
 * Renders resource activity timeline as colored segments with optional ticks
 */
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