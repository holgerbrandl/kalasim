package org.github.holgerbrandl.kalasim

import java.text.DecimalFormat


private val TRACE_DF = DecimalFormat("###.00")
private val TRACE_COL_WIDTHS = listOf(10, 25, 25, 12, 30)


data class TraceElement(
    val time: Double,
    val curComponent: Component?,
    val component: Component?,
    val info: String?
) {
    override fun toString(): String {

        return listOf(
            TRACE_DF.format(time),
            curComponent?.name,
            component?.name,
            component?.status?.toString() ?: "",
            info
        ).renderTraceLine().trim()
    }
}

private fun List<String?>.renderTraceLine(): String = map { (it ?: "") }
    .zip(TRACE_COL_WIDTHS)
    .map { (str, padLength) -> str.padEnd(padLength) }
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
                "component",
                "action",
                "info"
            )
            println(header.renderTraceLine())
            println(TRACE_COL_WIDTHS.map { "-".repeat(it - 1) }.joinToString(separator = " "))
        }

        // do a diff for logging
        val printElement = if(diffRecords && lastElement !=null){
            with(traceElement){
                val ccChanged = curComponent != lastElement!!.curComponent
                TraceElement(time, if(ccChanged) curComponent else null, if(ccChanged) null else component, info)
            }
        }else{
            traceElement
        }

        // update last element
        lastElement = traceElement

        println(printElement)
    }
}
