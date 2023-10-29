package org.kalasim.analysis

import com.github.holgerbrandl.jsonbuilder.json
import kotlinx.datetime.isDistantFuture
import org.json.JSONObject
import org.kalasim.*
import org.kalasim.misc.*
import kotlin.math.absoluteValue

enum class ResourceEventType { REQUESTED, CLAIMED, RELEASED, PUT, TAKE }


class ResourceEvent(
    time: SimTime,
    val requestId: Long,
    curComponent: Component?,
    val requester: Component,
    val resource: Resource,
    val type: ResourceEventType,
    val amount: Double,
    val priority: Priority?,
    val bumpedBy: Component? = null
) : InteractionEvent(time, curComponent, requester) {

    val claimed: Double = resource.claimed
    val capacity: Double = resource.capacity
    val occupancy: Double = resource.occupancy
    val requesters: Int = resource.requesters.size

    //    val requested: Int = resource.requesters.q.map{ it.component.requests.filter{ it.key == resource}}
    val claimers: Int = resource.claimers.size

    init {
        if(bumpedBy != null) require(type == ResourceEventType.RELEASED)
    }

    override val action: String
        get() {
            val prioInfo = if(priority != null) "with priority $priority" else ""

            //if this is to be disabled later it should include which resources were requested as part of oneOf=false
            // val honorInfo =  if(oneOf) "and oneOf=$oneOf" else ""

            return "${
                type.toString().lowercase().titlecaseFirstChar()
            } ${amount.absoluteValue.roundAny(2)} from '${resource.name}' $prioInfo".trim()
        }

    override fun toJson() = json {
        "eventType" to eventType
        "time" to time
        "request_id" to requestId
        "current" to current?.name
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

//internal fun Environment.asWtOptional(tickTime: TickTime) = if(hasAbsoluteTime()) toWallTime(tickTime) else null

data class ResourceActivityEvent(
    val requested: SimTime,
    val honored: SimTime,
    val released: SimTime,
    val requester: Component,
    val resource: Resource,
    val activity: String?,
    val quantity: Double,
) : Event(released) {
//    val requestedWT = resource.env.asWtOptional(requested)
//    val honoredWT = resource.env.asWtOptional(honored)
//    val releasedWT = resource.env.asWtOptional(released)

    override fun toJson() = json {
        "eventType" to eventType
        "requested" to requested
        "honored" to honored
        "released" to released
        "requester" to requester.name
        "resource" to resource.name
        "activity" to activity
        "quantity" to quantity
    }
}


open class InteractionEvent(
    time: SimTime,
    val current: Component? = null,
    val component: Component? = null,
    open val action: String? = null,
) : Event(time) {

    override fun toJson(): JSONObject = json {
        "eventType" to eventType
        "time" to time
        "current" to current?.name
        "receiver" to component?.name
        "action" to (action ?: "")
    }
}


/** Fired when a simulation state is changing its value. https://www.kalasim.org/state/ */
open class StateChangedEvent<T>(
    time: SimTime,
    val state: State<T>,
    val newValue: T,
    current: Component? = null,
    val trigger: Int? = null
) : InteractionEvent(time, current, null) {

    override val action: String?
        get() {
            return if(trigger != null) {
                "State changes to '$newValue' with trigger allowing $trigger components"
            } else {
                "State changed to '$newValue'"
            }
        }

    override fun toJson(): JSONObject = json {
        "eventType" to eventType
        "time" to time
        "current" to current?.name
        "state" to state.name
        "newValue" to newValue
        if(trigger != null) {
            "trigger" to trigger
        }
    }
}

class EntityCreatedEvent(
    time: SimTime,
    val creator: Component?,
    val entity: SimulationEntity,
    val details: String? = null
) : Event(time) {

    override fun toJson() = json {
        "eventType" to eventType
        "time" to time
        "creator" to creator?.name
        "entity" to entity.name
        "details" to details
    }
}

/** An event indicating that the state of  component has changed. See https://www.kalasim.org/component/#lifecycle */
open class ComponentStateChangeEvent(
    time: SimTime,
    current: Component? = null,
    component: Component,
    val state: ComponentState,
    details: String? = null
) : InteractionEvent(time, current, component, details) {

    override fun toJson(): JSONObject = json {
        "time" to time
        "type" to eventType
        "current" to current?.name
        "receiver" to component?.name
        "details" to (action ?: "")
        "state" to state
    }
}

/** An event indicating that a component process was scheduled for later execution or continuation.
 * See https://www.kalasim.org/component/#lifecycle
 */
class RescheduledEvent(
    time: SimTime,
    current: Component? = null,
    simEntity: Component,
    val description: String? = null,
    val scheduledFor: SimTime,
    val type: ScheduledType
) : ComponentStateChangeEvent(time, current, simEntity, ComponentState.SCHEDULED) {

    override val action: String
        get() {
            val extra = ", scheduled for ${formatWithInf(component?.env?.wall2TickTime(scheduledFor)!!)}"

            val delta = if(this.scheduledFor == time || (this.scheduledFor.isDistantFuture)) "" else {
                val scheduledIn = component.env.asTicks(scheduledFor - time)
                "+" + TRACE_DF.format(scheduledIn) + " "
            }

            val prettyType = when(type) {
                ScheduledType.WAIT -> "Waiting"
                ScheduledType.ACTIVATE -> "Activated"
                else -> type.toString().lowercase().titlecaseFirstChar()
            }

            return (if(!description.isNullOrBlank()) ("$description; ") else "") + ("$prettyType $delta").trim() + extra
        }
}

