package org.kalasim.analysis

import com.github.holgerbrandl.jsonbuilder.json
import org.json.JSONObject
import org.kalasim.*
import org.kalasim.misc.roundAny
import org.kalasim.misc.titlecaseFirstChar
import kotlin.math.absoluteValue

enum class ResourceEventType { REQUESTED, CLAIMED, RELEASED, PUT, TAKE }


class ResourceEvent(
    time: TickTime,
    val requestId: Long,
    curComponent: Component?,
    val requester: SimulationEntity,
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

    override fun renderAction(): String {
        val prioInfo = if(priority != null) "with priority $priority" else ""

        //if this is to be disabled later it should include which resources were requested as part of oneOf=false
//         val honorInfo =  if(oneOf) "and oneof=$oneOf" else ""

        return "${
            type.toString().lowercase().titlecaseFirstChar()
        } ${amount.absoluteValue.roundAny(2)} from '${resource.name}' $prioInfo".trim()
    }

    override fun toJson() = json {
        "time" to time
        "request_id" to requestId
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
    val requested: TickTime,
    val honored: TickTime,
    val released: TickTime,
    val requester: Component,
    val resource: Resource,
    val activity: String?,
    val claimedQuantity: Double,
) : Event(released) {
    val requestedWT = resource.env.tickTransform?.tick2wallTime(requested)
    val honoredWT = resource.env.tickTransform?.tick2wallTime(honored)
    val releasedWT = resource.env.tickTransform?.tick2wallTime(released)
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

    @Suppress("DEPRECATION")
    fun renderDetails() = details


    override fun toJson(): JSONObject = json {
        "time" to time
        "current" to curComponent?.name
        "receiver" to source?.name
        "action" to renderAction()
        "details" to renderDetails()
    }
}

class EntityCreatedEvent(
    time: TickTime,
    val creator: Component?,
    val entity: SimulationEntity,
    val details: String? = null
) : Event(time)

class ComponentStateChangeEvent(
    time: TickTime,
    curComponent: Component? = null,
    simEntity: SimulationEntity,
    state: ComponentState,
    details: String? = null
) : InteractionEvent(time, curComponent, simEntity, details, "New state: " + state.toString().lowercase())