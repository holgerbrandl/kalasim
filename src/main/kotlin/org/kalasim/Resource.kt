@file:Suppress("EXPERIMENTAL_API_USAGE")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.misc.Jsonable
import org.kalasim.monitors.NumericLevelMonitor
import org.koin.core.Koin
import org.kalasim.misc.KalasimContext

// TODO Analyze we we support the same preemptible contract as simmer (Ucar2019, p11) (in particular restart)


/**
 * A resource with a maximum `capacity` and a fill level for which claims are not tracked and do not need to be released.
 *
 * For full contract and description see [user manual](https://www.kalasim.org/resource/#depletable-resource)
 */
class DepletableResource(
    name: String? = null,
    capacity: Number,
    initialLevel: Number = capacity,
    preemptive: Boolean = false,
    koin: Koin = KalasimContext.get()
) : Resource(name = name, capacity = capacity, preemptive = preemptive,koin = koin) {

    init {
        anonymous = true
        claimed = capacity.toDouble() - initialLevel.toDouble()
    }
}

/**
 * @param preemptive If a component requests from a preemptive resource, it may bump component(s) that are claiming from
the resource, provided these have a lower priority = higher value). If component is bumped, it releases the resource and is the activated, thus essentially stopping the current action (usually hold or passivate). Therefore, it is necessary that a component claiming from a preemptive resource should check
whether the component is bumped or still claiming at any point where they can be bumped.
 */
open class Resource(
    name: String? = null,
    capacity: Number = 1,
    val preemptive: Boolean = false,
    koin: Koin = KalasimContext.get()
) : SimulationEntity(name = name, simKoin = koin) {

    internal var anonymous: Boolean = false

    var minq: Double = Double.MAX_VALUE

    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>("requesters of ${this.name}", koin = koin)
    val claimers = ComponentQueue<Component>("claimers of ${this.name}", koin = koin)

    var capacity = capacity.toDouble()
        set(value) {
            require(value >= claimed) { "can not reduce capacity below current claims" }

            field = value

            updateCapacityMonitors()
            tryRequest()
        }


    // https://stackoverflow.com/questions/41214452/why-dont-property-initializers-call-a-custom-setter
    var claimed: Double = 0.0
        internal set(x) {
            val diffQuantity = field - x

            field = x

            // this ugly hak is needed to avoid tracking of initialLevel setting in DR constructor
            if(this is DepletableResource && diffQuantity == 0.0 && requesters.isEmpty()) return

            if(field < EPS)
                field = 0.0

            claimedMonitor.addValue(x)
            updateCapacityMonitors()

//            if(this is DepletableResource && )
//            val action = if (diffQuantity > 0) "Released" else "Claimed"
//            log("$action ${diffQuantity.absoluteValue} from '$name'")

            // it would seem natural to tryReqeust here, but since this is not done in case of bumping,
            // we do it manually at the call-site instead
        }


    private fun updateCapacityMonitors() {
        availableMonitor.addValue(availableQuantity)
        occupancyMonitor.addValue(occupancy)
        capacityMonitor.addValue(capacity)
    }

    val occupancy: Double
        get() = if(capacity < 0) 0.0 else claimed / capacity

    val availableQuantity: Double
        get() = capacity - claimed


    val capacityMonitor = NumericLevelMonitor("Capacity of ${super.name}", initialValue = capacity, koin = koin)
    val claimedMonitor = NumericLevelMonitor("Claimed quantity of ${this.name}", koin = koin)
    val availableMonitor =
        NumericLevelMonitor("Available quantity of ${this.name}", initialValue = availableQuantity, koin = koin)
    val occupancyMonitor = NumericLevelMonitor("Occupancy of ${this.name}", koin = koin)


    init {
        logInternal(
            env.now,
            env.curComponent,
            this,
            "Created",
            "capacity=$capacity " + if(anonymous) "anonymous" else ""
        )
    }

    fun tryRequest(): Boolean {
        val iterator = requesters.q.iterator()

        if(anonymous) {
            // TODO trying not implemented

            iterator.forEach {
                it.component.tryRequest()
            }
        } else {
            while(iterator.hasNext()) {
                //try honor as many requests as possible
                if(minq > (capacity - claimed + EPS)) {
                    break
                }
                iterator.next().component.tryRequest()
            }
        }

        return true
    }

    /** Releases all claims or a specified quantity
     *
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(quantity: Number? = null) {
        // TODO Split resource types into QuantityResource and Resource or similar
        if(anonymous) {
            val q = quantity?.toDouble() ?: claimed

            claimed = -q

            // done within decrementing claimedQuantity
//            occupancyMonitor.addValue(if (capacity <= 0) 0 else claimedQuantity / capacity)
//            availableQuantityMonitor.addValue(capacity - claimedQuantity)

            tryRequest()

        } else {
            require(quantity != null) { "quantity missing for non-anonymous resource" }

            while(claimers.isNotEmpty()) {
                claimers.q.first().component.release(this)
            }
        }
    }

    fun removeRequester(component: Component) {
        requesters.remove(component)
        if(requesters.isEmpty()) minq = Double.MAX_VALUE
    }

    /** Prints a summary of statistics of a resource. */
    fun printStatistics() = println(statistics.toString())

    override val info: Jsonable
        get() = ResourceInfo(this)

    val statistics: ResourceStatistics
        get() = ResourceStatistics(this)
}


class ResourceInfo(resource: Resource) : Jsonable() {
    val name: String = resource.name
    val creationTime: TickTime = resource.creationTime

    val claimedQuantity: Double = resource.claimed
    val capacity = resource.capacity

    // use a dedicated type here to see null prios in json
    val claimedBy = resource.claimers.q.toList().map { it.component.name to it.priority }

    data class ReqComp(val component: String, val quantity: Double?)

    val requestingComponents = resource.requesters.q.toList().map {
        ReqComp(it.component.name, it.component.requestedQuantity(resource))
    }
}


@Suppress("MemberVisibilityCanBePrivate")
class ResourceStatistics(resource: Resource) : Jsonable() {

    val name = resource.name
    val timestamp = resource.env.now

    val requesters = resource.requesters.stats
    val claimers = resource.claimers.stats

    val capacity = resource.capacityMonitor.statistics(false)
    val availableQuantity = resource.availableMonitor.statistics(false)
    val claimedQuantity = resource.claimedMonitor.statistics(false)
    val occupancy = resource.occupancyMonitor.statistics(false)


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



