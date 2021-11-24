package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import mu.KotlinLogging
import org.json.JSONObject
import org.kalasim.misc.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.logging.Level
import kotlin.math.absoluteValue


enum class ResourceEventType { CLAIMED, RELEASED, PUT }

class ResourceEvent(
    time: TickTime,
    curComponent: Component?,
    val requester: SimulationEntity,
    val resource: Resource,
    val type: ResourceEventType,
    val amount: Double
) : InteractionEvent(time, curComponent, requester) {

    val claimed: Double = resource.claimed
    val capacity: Double = resource.capacity
    val occupancy: Double = resource.occupancy
    val requesters: Int = resource.requesters.size

    //    val requested: Int = resource.requesters.q.map{ it.component.requests.filter{ it.key == resource}}
    val claimers: Int = resource.claimers.size


    override fun renderAction() =
        "${
            type.toString().lowercase(Locale.ENGLISH).capitalize()
        } ${amount.absoluteValue.roundAny(2)} from '${requester.name}'"

    override fun toJson() = json {
        "time" to time
        "current" to curComponent?.name
        "requester" to requester.name
        "resource" to resource.name
        "type" to type
        "amount" to amount
        "capacity" to capacity
        "claimed" to claimed
        "occupancy" to occupancy
        "requesters" to requesters
        "claimers" to claimers
    }

}

data class ResourceActivityEvent(
    val start: TickTime,
    val end: TickTime,
    val requester: Component,
    val resource: Resource,
    val activity: String?,
    val claimedQuantity: Double
) : Event(end) {
    val startWT = resource.env.tickTransform?.tick2wallTime(start)
    val endWT = resource.env.tickTransform?.tick2wallTime(end)
}


open class InteractionEvent(
    time: TickTime,
    val curComponent: Component? = null,
    val source: SimulationEntity? = null,
    val action: String? = null,
    @Deprecated("Will be removed because unclear semantics in comparison to action parameter")
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
    val time: TickTime
) : Jsonable() {

//    constructor(time: TickTime) : this(time.value)
//    constructor(time: Number) : this(time.toDouble())

    open val logLevel: Level get() = Level.INFO

    override fun toString() = toJson().toString()


    override fun toJson(): JSONObject = json {
        "time" to time
        "type" to this@Event.javaClass.simpleName
    }
}

class EntityCreatedEvent(
    time: TickTime,
    val creator: Component?,
    val simEntity: SimulationEntity,
    val details: String? = null
) : Event(time)

class ComponentStateChangeEvent(
    time: TickTime,
    curComponent: Component? = null,
    simEntity: SimulationEntity,
    state: ComponentState,
    details: String? = null
) : InteractionEvent(time, curComponent,simEntity, details, "New state: "+ state.toString().lowercase())


fun interface EventListener {
    fun consume(event: Event)

//    val filter: EventFilter?
//        get() = null
}


fun interface EventFilter {
    fun matches(event: Event): Boolean
}


class ConsoleTraceLogger(var logLevel: Level = Level.INFO) : EventListener {


    enum class TraceTableColumn { time, current, receiver, action, info }

    companion object {
        internal val TRACE_COL_WIDTHS = mutableListOf(10, 22, 22, 65, 35)

        fun setColumnWidth(column: TraceTableColumn, width: Int) = when (column) {
            TraceTableColumn.time -> TRACE_COL_WIDTHS[0] = width
            TraceTableColumn.current -> TRACE_COL_WIDTHS[1] = width
            TraceTableColumn.receiver -> TRACE_COL_WIDTHS[2] = width
            TraceTableColumn.action -> TRACE_COL_WIDTHS[3] = width
            TraceTableColumn.info -> TRACE_COL_WIDTHS[4] = width
        }
    }


    var hasPrintedHeader = false
    var lastCurrent: Component? = null
    var lastReceiver: SimulationEntity? = null



    override fun consume(event: Event) {
        if (event.logLevel.intValue() < logLevel.intValue()) return

        if (!hasPrintedHeader) {
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

            val traceLine: List<String?> = when (this) {
                is InteractionEvent -> {
                    val ccChanged = curComponent != lastCurrent
                    val receiverChanged = source != lastReceiver

                    listOf(
                        TRACE_DF.format(time.value),
                        if (ccChanged) curComponent?.name else null,
                        if (receiverChanged) source?.name else null,
        //                ((source?.name ?: "") + " " + (renderAction() ?: "")).trim(),
                        renderAction().capitalize(),
                        renderDetails()
                    ).apply {
                        // update last element
                        lastCurrent = this@with.curComponent
                        lastReceiver = this@with.source
                    }
                }
                is EntityCreatedEvent -> {
                    val ccChanged = creator != lastCurrent

                    listOf(TRACE_DF.format(time.value), if (ccChanged) creator?.name else null, simEntity.name , "Created", details).apply {
                        // update last element
                        lastCurrent = this@with.creator
                        lastReceiver = this@with.simEntity
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


    private fun List<String?>.renderTraceLine(): String = map { (it ?: "") }
        .zip(TRACE_COL_WIDTHS)
        .map { (str, padLength) ->
            val padded = str.padEnd(padLength)
            if (str.length >= padLength) {
                padded.dropLast(str.length - padLength + 5) + "... "
            } else padded
        }
        .joinToString("")
}

/** Collects all events on the kalasim event bus. */
fun Environment.traceCollector(): TraceCollector {
    val tc = dependency { TraceCollector() }
    addEventListener(tc)

    return tc
}

class TraceCollector(val traces: MutableList<Event> = mutableListOf()) : EventListener,
    MutableList<Event> by traces {
//    val traces = mutableListOf<Event>()

    override fun consume(event: Event) {
        traces.add(event)
    }

    operator fun invoke() = traces
//    operator fun get(index: Int): Event = traces[index]
}


inline fun <reified E : Event> Environment.eventCollector(): List<E> {
    val traces: MutableList<E> = mutableListOf()

    addEventListener {
        if (it is E) traces.add(it)
    }

    return traces.toList()
}
