package org.github.holgerbrandl.kalasim;

import kotlinx.serialization.Serializable

/**
 * @param preemptive If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority = higher value). If component is bumped, it releases the resource and is the activated, thus essentially stopping the current action (usually hold or passivate). Therefore, it is necessary that a component claiming from a preemptive resource should check
whether the component is bumped or still claiming at any point where they can be bumped.
 */
open class Resource(
    name: String? = null,
    var capacity: Double = 1.0,
    val preemptive: Boolean = false,
    val anonymous: Boolean = false
) : SimulationEntity(name = name) {


    var minq: Double = Double.MAX_VALUE

    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>()
    val claimers = ComponentQueue<Component>()

    var claimedQuantity = 0.0
        set(x) {
            field = x

            claimedQuantityMonitor.addValue(x)
            availableQuantityMonitor.addValue(capacity - claimedQuantity)
            occupancyMonitor.addValue(if (capacity < 0) 0 else claimedQuantity)

            env.printTrace("claim ${claimedQuantity} from $name")
        }


    var availableQuantity = 0
        set(x) {
            field = x
            availableQuantityMonitor.addValue(x)
        }


    val claimedQuantityMonitor = NumericLevelMonitor("Claimed quantity of $name")
    val availableQuantityMonitor = NumericLevelMonitor("Available quantity of $name")
    val occupancyMonitor = NumericLevelMonitor("Occupancy of $name")

    val capacityMonitor = NumericLevelMonitor("Capacity of ${name}").apply {
        addValue(capacity)
    }

    init {
        env.printTrace("create ${this.name} with capcacity ${capacity} "+ if(anonymous) "anonymous" else "")
    }

    fun availableQuantity(): Double = capacity - claimedQuantity



    fun tryRequest(): Boolean {
        val iterator = requesters.q.iterator()

        if (anonymous) {
            // TODO trying not implemented

            iterator.forEach {
                it.c.tryRequest()
            }
        } else {
            while (iterator.hasNext()) {
                //try honor as many requests as possible
                if (minq > (capacity - claimedQuantity + EPS)) {
                    break
                }
                iterator.next().c.tryRequest()
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

    override val info: Snapshot
        get() = ResourceInfo(this)
}

@Serializable
open class ResourceInfo : Snapshot {

    constructor(c: Resource) : super() {
        this.name = c.name
        this.creationTime = c.creationTime
        this.claimers = c.claimers.q.toList().map { it.c.name to it.priority }
        this.requesters = c.requesters.q.toList().map { it.c.name to it.priority }
    }

    val name: String
    val creationTime: Double

    val claimers: List<Pair<String, Int?>>
    val requesters: List<Pair<String, Int?>>
}
