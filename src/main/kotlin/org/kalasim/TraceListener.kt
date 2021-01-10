package org.kalasim

import org.kalasim.misc.Jsonable
import org.kalasim.misc.TRACE_DF
import java.util.logging.Level


internal val TRACE_COL_WIDTHS = mutableListOf(10, 25, 45, 35)

abstract class TraceDetails : Jsonable()

class RequestClaimed(


) : TraceDetails() {

}

class TraceElement(
    time: Double,
    curComponent: Component?,
    source: SimulationEntity?,
    val action: String?,
    val details: String?
) : AbstractTraceElement(time, curComponent, source) {

    override fun renderAction() = action
    override fun renderDetails() = details
}

//todo we should provide an api to resolve these references into a more slim log representation
abstract class AbstractTraceElement(
    val time: Double,
    val curComponent: Component?,
    val source: SimulationEntity?
) : Jsonable() {

    abstract fun renderAction(): String?
    abstract fun renderDetails(): String?

    override fun toString(): String {

        return listOf(
            TRACE_DF.format(time),
            curComponent?.name,
            ((source?.name ?: "") + " " + (renderAction() ?: "")).trim(),
//            if(source is Component) source.status.toString() else "",
            renderDetails()
        ).apply {
            1 + 1
        }.renderTraceLine().trim()
    }
}

private fun List<String?>.renderTraceLine(): String = map { (it ?: "") }
    .zip(TRACE_COL_WIDTHS)
    .map { (str, padLength) ->
        val padded = str.padEnd(padLength)
        if (str.length >= padLength) {
            padded.dropLast(str.length - padLength + 5) + "... "
        } else padded
    }
    .joinToString("")


fun interface TraceListener {
    fun processTrace(traceElement: TraceElement)
}

fun interface TraceFilter {
    fun matches(te: TraceElement): Boolean
}

class ConsoleTraceLogger(val diffRecords: Boolean, var logLevel: Level = Level.FINER) : TraceListener {

    var hasPrintedHeader = false
    var lastElement: TraceElement? = null


    override fun processTrace(traceElement: TraceElement) {
        if (!hasPrintedHeader) {
            hasPrintedHeader = true

            val header = listOf(
                "time",
                "current component",
//                "state",
                "action",
                "info"
            )
            println(header.renderTraceLine())
            println(TRACE_COL_WIDTHS.map { "-".repeat(it - 1) }.joinToString(separator = " "))
        }

        // do a diff for logging
        val printElement = if (diffRecords && lastElement != null) {
            with(traceElement) {
                val ccChanged = curComponent != lastElement!!.curComponent
                TraceElement(
                    time,
                    if (ccChanged) curComponent else null,
                    if (ccChanged) null else source,
                    action,
                    details
                )
            }
        } else {
            traceElement
        }

        // update last element
        lastElement = traceElement

        println(printElement)
    }
}

fun Environment.traceCollector() = TraceCollector().apply { addTraceListener(this) }

class TraceCollector() : TraceListener {
    val traces = mutableListOf<TraceElement>()

    override fun processTrace(traceElement: TraceElement) {
        traces.add(traceElement)
    }

    operator fun get(index: Int): TraceElement = traces[index]
}