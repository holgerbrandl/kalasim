package org.kalasim.analysis

import org.kalasim.*
import org.kalasim.misc.TRACE_DF
import org.kalasim.misc.titlecaseFirstChar
import java.util.logging.Level

class ConsoleTraceLogger(var logLevel: Level = Level.INFO) : EventListener {

    enum class EventsTableColumn { Time, Current, Receiver, Action, Info }

    companion object {
        val TRACE_COL_WIDTHS = mutableListOf(10, 22, 22, 55, 35)

        fun setColumnWidth(column: EventsTableColumn, width: Int) = when(column) {
            EventsTableColumn.Time -> TRACE_COL_WIDTHS[0] = width
            EventsTableColumn.Current -> TRACE_COL_WIDTHS[1] = width
            EventsTableColumn.Receiver -> TRACE_COL_WIDTHS[2] = width
            EventsTableColumn.Action -> TRACE_COL_WIDTHS[3] = width
            EventsTableColumn.Info -> TRACE_COL_WIDTHS[4] = width
        }
    }


    var hasPrintedHeader = false
    var lastCurrent: Component? = null
    var lastReceiver: SimulationEntity? = null


    override fun consume(event: Event) {
        if(event.logLevel.intValue() < logLevel.intValue()) return

        if(!hasPrintedHeader) {
            hasPrintedHeader = true

            val header = listOf(
                "time",
                "current",
                "receiver",
                "action",
                "info"
            )
            println(header.renderTraceLine())
            println(TRACE_COL_WIDTHS.joinToString(separator = " ") { "-".repeat(it - 1) })
        }


        with(event) {

            val traceLine: List<String?> = when(this) {
                is InteractionEvent -> {
                    val ccChanged = curComponent != lastCurrent
                    val receiverChanged = source != lastReceiver

                    listOf(
                        TRACE_DF.format(time.value),
                        if(ccChanged) curComponent?.name else null,
                        if(receiverChanged) source?.name else null,
                        //                ((source?.name ?: "") + " " + (renderAction() ?: "")).trim(),
                        renderAction().titlecaseFirstChar(),
                        renderDetails()
                    ).apply {
                        // update last element
                        lastCurrent = this@with.curComponent
                        lastReceiver = this@with.source
                    }
                }
                is EntityCreatedEvent -> {
                    val ccChanged = creator != lastCurrent

                    listOf(
                        TRACE_DF.format(time.value),
                        if(ccChanged) creator?.name else null,
                        entity.name,
                        "Created",
                        details
                    ).apply {
                        // update last element
                        lastCurrent = this@with.creator
                        lastReceiver = this@with.entity
                    }
                }
                else -> {
                    listOf(TRACE_DF.format(time.value), "", "", toString())
                }
            }

            val renderedLine = traceLine.renderTraceLine().trim()

            println(renderedLine)
        }
    }


    private fun List<String?>.renderTraceLine(): String =
        map { (it ?: "") }
            .zip(TRACE_COL_WIDTHS)
            .joinToString("") { (str, padLength) ->
                val padded = str.padEnd(padLength)
                if(str.length >= padLength) {
                    padded.dropLast(str.length - padLength + 5) + "... "
                } else padded
            }
}