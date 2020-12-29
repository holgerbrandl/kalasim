package org.kalasim

import org.kalasim.misc.TRACE_DF


private val TRACE_COL_WIDTHS = listOf(10, 25, 45, 35)

//todo we should provide an api to resolve these references into a more slim log representation
data class TraceElement(
    val time: Double,
    val curComponent: Component?,
    val source: SimulationEntity?,
    val action: String?,
    val info: String?
) {
    override fun toString(): String {

        return listOf(
            TRACE_DF.format(time),
            curComponent?.name,
            ((source?.name ?: "") + " " + (action ?: "")).trim(),
//            if(source is Component) source.status.toString() else "",
            info
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

class ConsoleTraceLogger(val diffRecords: Boolean) : TraceListener {

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
                    info
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
