package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.json.JSONObject
import org.kalasim.misc.Jsonable
import org.kalasim.misc.TRACE_DF
import java.util.logging.Level
import kotlin.math.absoluteValue


internal val TRACE_COL_WIDTHS = mutableListOf(10, 22, 22, 45, 35)

abstract class TraceDetails : Jsonable()

//inline class SimTime(val time: Double)

enum class ResourceEventType { CLAIMED, RELEASED, PUT }

class ResourceEvent(
    time: Double,
    val requester: SimulationEntity,
    val resource: Resource,
    curComponent: Component?,
    val type: ResourceEventType,
    val amount: Double,
    val capacity: Double,
    val claimed: Double
) : InteractionEvent(time, curComponent, requester) {

    override fun renderAction() =
        "${type.toString().toLowerCase().capitalize()} ${amount.absoluteValue.roundAny(2)} from '${requester.name}'"


}

open class InteractionEvent(
    time: Double,
    val curComponent: Component? = null,
    val source: SimulationEntity? = null,
    val action: String? = null,
    val details: String? = null
) : Event(time) {

    open fun renderAction() = action ?: ""

    fun renderDetails() = details


    override fun toJson(): JSONObject = json {
        "time" to time
        "current" to curComponent?.name
        "receiver" to source?.name
        "action" to renderAction()
        "details" to renderDetails()
    }

}

/** The base event of kalasim. Usually this extended to convey more specific information.*/
abstract class Event(
    val time: Double
) : Jsonable() {

    open val logLevel: Level get() = Level.INFO

    override fun toJson(): JSONObject = json {
        "time" to time
        "details" to toString()
    }
}


fun interface EventListener {
    fun consume(event: Event)

    val filter: EventFilter?
        get() = null
}

fun interface EventFilter {
    fun matches(event: Event): Boolean
}

class ConsoleTraceLogger(val diffRecords: Boolean, var logLevel: Level = Level.INFO) : EventListener {

    var hasPrintedHeader = false
    var lastElement: InteractionEvent? = null


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
            println(TRACE_COL_WIDTHS.map { "-".repeat(it - 1) }.joinToString(separator = " "))
        }


        with(event) {

            val traceLine: List<String?> = if(this is InteractionEvent) {
                val ccChanged = curComponent != lastElement?.curComponent
                val receiverChanged = source != lastElement?.source

                listOf(
                    TRACE_DF.format(time),
                    if(ccChanged) curComponent?.name else null,
                    if(receiverChanged) source?.name else null,
//                ((source?.name ?: "") + " " + (renderAction() ?: "")).trim(),
                    renderAction(),
                    renderDetails()
                ).apply {
                    // update last element
                    lastElement = this@with
                }
            } else {
                listOf(TRACE_DF.format(time), "", "", toString())
            }

            val renderedLine = traceLine.renderTraceLine().trim()

            println(renderedLine)
        }
    }


    private fun List<String?>.renderTraceLine(): String = map { (it ?: "") }
        .zip(TRACE_COL_WIDTHS)
        .map { (str, padLength) ->
            val padded = str.padEnd(padLength)
            if(str.length >= padLength) {
                padded.dropLast(str.length - padLength + 5) + "... "
            } else padded
        }
        .joinToString("")
}

fun Environment.traceCollector(): TraceCollector {
    val tc = dependency { TraceCollector() }
    addEventListener(tc)

    return tc
}

class TraceCollector(val traces: MutableList<Event> = mutableListOf<Event>()) : EventListener,
    MutableList<Event> by traces {
//    val traces = mutableListOf<Event>()

    override fun consume(event: Event) {
        traces.add(event)
    }

    operator fun invoke() = traces
//    operator fun get(index: Int): Event = traces[index]
}