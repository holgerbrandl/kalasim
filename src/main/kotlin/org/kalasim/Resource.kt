package org.kalasim;

import com.systema.analytics.es.misc.json
import org.kalasim.misc.Jsonable

/**
 * @param preemptive If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority = higher value). If component is bumped, it releases the resource and is the activated, thus essentially stopping the current action (usually hold or passivate). Therefore, it is necessary that a component claiming from a preemptive resource should check
whether the component is bumped or still claiming at any point where they can be bumped.
 */
open class Resource(
    name: String? = null,
    capacity: Number = 1,
    val preemptive: Boolean = false,
    val anonymous: Boolean = false
) : SimulationEntity(name = name) {


    var minq: Double = Double.MAX_VALUE

    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>("requesters of ${this.name}")
    val claimers = ComponentQueue<Component>("claimers of ${this.name}")

    var capacity = capacity.toDouble()
        set(value) {
            field = value
        }

    var claimedQuantity = 0.0
        set(x) {
            field = x

            claimedQuantityMonitor.addValue(x)
            availableQuantityMonitor.addValue(capacity - claimedQuantity)
            occupancyMonitor.addValue(if (capacity < 0) 0 else claimedQuantity / capacity)
            capacityMonitor.addValue(capacity)

            printTrace("claim $claimedQuantity from $name")
        }


    val availableQuantity: Double
        get() = capacity - claimedQuantity


    // todo TBD should we initialize these monitoring by tallying the intial state?
    val capacityMonitor = NumericLevelMonitor("Capacity of ${super.name}", initialValue = capacity)
    val claimedQuantityMonitor = NumericLevelMonitor("Claimed quantity of ${this.name}")
    val availableQuantityMonitor = NumericLevelMonitor("Available quantity of ${this.name}", initialValue = availableQuantity)
    val occupancyMonitor = NumericLevelMonitor("Occupancy of ${this.name}")


    init {
        printTrace("create ${this.name} with capacity $capacity " + if (anonymous) "anonymous" else "")
    }

    fun tryRequest(): Boolean {
        val iterator = requesters.q.iterator()

        if (anonymous) {
            // TODO trying not implemented

            iterator.forEach {
                it.component.tryRequest()
            }
        } else {
            while (iterator.hasNext()) {
                //try honor as many requests as possible
                if (minq > (capacity - claimedQuantity + EPS)) {
                    break
                }
                iterator.next().component.tryRequest()
            }
        }

        return true
    }

    /** releases all claims or a specified quantity
     *
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(quantity: Double? = null) {
        // TODO Split resource types into QuantityResource and Resource or similar
        if (anonymous) {
            val q = quantity ?: claimedQuantity

            claimedQuantity = -q
            if (claimedQuantity < EPS) claimedQuantity = 0.0

            // done within decrementing claimedQuantity
//            occupancyMonitor.addValue(if (capacity <= 0) 0 else claimedQuantity / capacity)
//            availableQuantityMonitor.addValue(capacity - claimedQuantity)

        } else {
            require(quantity != null) { "quantity missing for non-anonymous resource" }

            while (requesters.isNotEmpty()) {
                requesters.poll().release(this)
            }
        }
    }

    fun removeRequester(component: Component) {
        requesters.remove(component)
        if (requesters.isEmpty()) minq = Double.MAX_VALUE
    }

    /** prints a summary of statistics of a resource */
    fun printStatistics() {
        println(statistics.toString())
    }

    override val info: Jsonable
        get() = ResourceInfo(this)

    val statistics: ResourceStatistics
        get() = ResourceStatistics(this)
}


class ResourceInfo(resource: Resource) : Jsonable() {
    val name: String = resource.name
    val creationTime: Double = resource.creationTime

    val claimedQuantity: Double = resource.claimedQuantity
    val capacity = resource.capacity

    // use a dedicated type here to see null prios in json
    val claimedBy = resource.claimers.q.toList().map { it.component.name to it.priority }

    data class ReqComp(val component:String, val quantity:Double?)
    val requestingComponents = resource.requesters.q.toList().map {
        ReqComp(it.component.name,it.component.requests[resource])
    }
}



@Suppress("MemberVisibilityCanBePrivate")
class ResourceStatistics(resource: Resource) : Jsonable() {

    val name = resource.name
    val timestamp = resource.env.now

    val requesters = resource.requesters.stats
    val claimers = resource.claimers.stats

    val capacity = NumericLevelMonitorStats(resource.capacityMonitor)
    val availableQuantity = NumericLevelMonitorStats(resource.availableQuantityMonitor)
    val claimedQuantity = NumericLevelMonitorStats(resource.claimedQuantityMonitor)
    val occupancy = NumericLevelMonitorStats(resource.occupancyMonitor)


    override fun toJson() = json {
        "name" to name
        "timestamp" to timestamp
        "type" to this@ResourceStatistics.javaClass.simpleName

        "requesterStats" to requesters.toJson()
        "claimerStats" to claimers.toJson()
        
        "capacity" to capacity.toJson()
        "availableQuantity" to availableQuantity.toJson()
        "claimedQuantity" to claimedQuantity.toJson()
        "occupancy" to occupancy.toJson()
    }
}
