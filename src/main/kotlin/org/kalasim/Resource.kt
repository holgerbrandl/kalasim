@file:Suppress("EXPERIMENTAL_API_USAGE")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import krangl.*
import org.kalasim.monitors.MetricTimeline
import org.kalasim.monitors.div
import org.kalasim.monitors.minus
import org.koin.core.Koin
import org.kalasim.misc.*
import org.kalasim.monitors.copy

// TODO Analyze we we support the same preemptible contract as simmer (Ucar2019, p11) (in particular restart)


/** See [user manual](https://www.kalasim.org/resource/#request-honor-policies). */
sealed class RequestHonorPolicy {
    object StrictFCFS : RequestHonorPolicy()
    object RelaxedFCFS : RequestHonorPolicy()
    object SQF : RequestHonorPolicy()

    class WeightedSQF(val alpha: Double) : RequestHonorPolicy() {
        init {
            require(alpha in 0.0..1.0) { "alpha must be between 0 and 1 " }
        }
    }
}



/**
 * A resource with a maximum `capacity` and a fill `level` for which claims are not tracked and do not need to be released. For full contract and description see [user manual](https://www.kalasim.org/resource/#depletable-resource)
 *
 * @param honorPolicy The order in which multiple competing resource requests are honored. See [honor policies](https://www.kalasim.org/resource/#request-honor-policies)
 */
open class DepletableResource(
    name: String? = null,
    capacity: Number,
    initialLevel: Number = capacity,
    honorPolicy: RequestHonorPolicy = RequestHonorPolicy.StrictFCFS,
    preemptive: Boolean = false,
    koin: Koin = DependencyContext.get()
) : Resource(
    name = name,
    capacity = capacity,
    honorPolicy = honorPolicy,
    preemptive = preemptive,
    koin = koin
) {

    /** Indicates if depletable resource is at full capacity. */
    val isFull: Boolean
        get() = claimed == capacity

    /** Indicates if depletable resource is depleted (level==0) */
    val isDepleted
        get() = level == 0.0


    /** Indicates the current level of the resource. Technically is a synonym for `capacity - claimed` */
    val level
        get() = capacity - claimed

//    val levelTimeline = MetricTimeline("Level of ${this.name}", initialValue = availableQuantity, koin = koin)
//
//    override fun updatedDerivedMetrics() {
//        levelTimeline.addValue(level)
//    }

    val levelTimeline
        get() = (capacityTimeline - claimedTimeline).copy("Level of ${this@DepletableResource.name}")


    init {
        depletable = true
        claimed = capacity.toDouble() - initialLevel.toDouble()
    }
}


/**
 * A simulation entity with a limited capacity, that can be requested by other simulation components. For details see https://www.kalasim.org/resource/
 *
 * @param honorPolicy The order in which multiple competing resource requests are honored. See [honor policies](https://www.kalasim.org/resource/#request-honor-policies)
 * @param preemptive Indicates if a resource should allow preemptive requests. See [preemptive resources](https://www.kalasim.org/resource/#pre-emptive-resources).
 */
open class Resource(
    name: String? = null,
    capacity: Number = 1,
    val honorPolicy: RequestHonorPolicy = RequestHonorPolicy.StrictFCFS,
    val preemptive: Boolean = false,
    koin: Koin = DependencyContext.get()
) : SimulationEntity(name = name, simKoin = koin) {

    internal var depletable: Boolean = false

    var minq: Double = Double.MAX_VALUE

    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>("requesters of ${this.name}", koin = koin)
    val claimers = ComponentQueue<Component>("claimers of ${this.name}", koin = koin)

    var capacity = capacity.toDouble()
        set(newCapacity) {
            if (newCapacity < claimed) {
                throw CapacityExceededException(this, "can not reduce capacity below current claims", now, newCapacity)
            }

            val capacityDiff = newCapacity - field
            field = newCapacity

            capacityTimeline.addValue(capacity)
//            updatedDerivedMetrics()

            if (this is DepletableResource) {
                // maintain the fill level of depletable resources
                claimed += capacityDiff
            }

            tryRequest()
        }


    // https://stackoverflow.com/questions/41214452/why-dont-property-initializers-call-a-custom-setter
    var claimed: Double = 0.0
        internal set(x) {
            val diffQuantity = field - x

            field = x

            // this ugly hak is needed to avoid tracking of initialLevel setting in DR constructor
            if (this is DepletableResource && diffQuantity == 0.0 && requesters.isEmpty()) return

            if (field < EPS)
                field = 0.0

            claimedTimeline.addValue(x)
//            updatedDerivedMetrics()

//            if(this is DepletableResource && )
//            val action = if (diffQuantity > 0) "Released" else "Claimed"
//            log("$action ${diffQuantity.absoluteValue} from '$name'")

            // it would seem natural to tryReqeust here, but since this is not done in case of bumping,
            // we do it manually at the call-site instead
        }


    val occupancy: Double
        get() = if (capacity < 0) 0.0 else claimed / capacity

    val availableQuantity: Double
        get() = capacity - claimed


    val capacityTimeline = MetricTimeline("Capacity of ${super.name}", initialValue = capacity, koin = koin)
    val claimedTimeline = MetricTimeline("Claimed quantity of ${this.name}", koin = koin)

    //    val availabilityTimeline =
//            MetricTimeline("Available quantity of ${this.name}", initialValue = availableQuantity, koin = koin)
//    val occupancyTimeline = MetricTimeline("Occupancy of ${this.name}", koin = koin)
    val availabilityTimeline
        get() = (capacityTimeline - claimedTimeline) //.also { it.name = "Occupancy of ${this.name}"}

    //    val occupancyTimeline = MetricTimeline("Occupancy of ${this.name}", koin = koin)
    val occupancyTimeline
        get() = claimedTimeline / capacityTimeline


    var trackingPolicy: ResourceTrackingConfig = ResourceTrackingConfig()
        set(newPolicy) {
            field = newPolicy

            with(newPolicy) {
                capacityTimeline.enabled = trackUtilization
                claimedTimeline.enabled = trackUtilization

//                availabilityTimeline.enabled = trackUtilization
//                occupancyTimeline.enabled = trackUtilization

                requesters.lengthOfStayTimeline.enabled = trackQueueStatistics
                requesters.queueLengthTimeline.enabled = trackQueueStatistics
                claimers.lengthOfStayTimeline.enabled = trackQueueStatistics
                claimers.queueLengthTimeline.enabled = trackQueueStatistics
            }
        }

    init {
        trackingPolicy = env.trackingPolicyFactory.getPolicy(this)
    }


    init {
        log(trackingPolicy.logCreation) {
            EntityCreatedEvent(now, env.curComponent, this, "capacity=$capacity " + if (depletable) "anonymous" else "")
        }
    }

    fun tryRequest(): Boolean {
        if (depletable) {
            // note: trying seems to lack any function in salabim; It is always reset after the while loop in a tryrequest
            do {
                val wasHonored = with(requesters.q) {
                    isNotEmpty() && peek().component.tryRequest()
                }
//                println(wasHonored)
            } while (wasHonored)
        } else {
            while (requesters.q.isNotEmpty()) {
                //try honor as many requests as possible
                if (minq > (capacity - claimed + EPS)) {
                    break
                }

                if (!requesters.q.peek().component.tryRequest()) {
                    // if we can honor this request, we must stop here (to respect request prioritites
                    break
                }
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
        if (depletable) {
            val q = quantity?.toDouble() ?: claimed

            claimed = -q

            // done within decrementing claimedQuantity
//            occupancyTimeline.addValue(if (capacity <= 0) 0 else claimedQuantity / capacity)
//            availableQuantityMonitor.addValue(capacity - claimedQuantity)

            tryRequest()

        } else {
            require(quantity != null) { "quantity missing for non-anonymous resource" }

            while (claimers.isNotEmpty()) {
                claimers.q.first().component.release(this)
            }
        }
    }

    fun removeRequester(component: Component) {
        requesters.remove(component)
        if (requesters.isEmpty()) minq = Double.MAX_VALUE
    }

    /** Prints a summary of statistics of a resource. */
    fun printStatistics() = println(statistics.toString())

    override val info: Jsonable
        get() = ResourceInfo(this)

    val statistics: ResourceStatistics
        get() = ResourceStatistics(this)


    val activities: List<ResourceActivityEvent> = mutableListOf()
//    val timeline = env.eventCollector<RequestScopeEvent>()

}


//
// Resource timeline for streamlined analytics
//

enum class ResourceMetric { Capacity, Claimed, Requesters, Claimers, Occupancy, Availability }

data class ResourceTimelineSegment(
    val resource: Resource,
    val start: TickTime,
    val end: TickTime?,
    val duration: Double?,
    val metric: ResourceMetric,
    val value: Double,
//    val start_wt: Instant? = null,
//    val end_wt: Instant? = null
) {
//    @Suppress("unused")
//    // needed by krangl if no tick-transform is defined
//    constructor(
//        resource : Resource,
//        duration: Double,
//        start: TickTime,
//        end: TickTime,
//        value: Double,
//        metric: ResourceMetric
//    ) : this(resource, start, end,duration,  metric,value, null, null) {
//
//    }

    val startWT = resource.env.tickTransform?.tick2wallTime(start)
    val endWT = end?.let { resource.env.tickTransform?.tick2wallTime(it) }
}


val Resource.timeline: List<ResourceTimelineSegment>
    get() {
        val capStats = capacityTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Capacity }
        val claimStats = claimedTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Claimed }
        val occStats = occupancyTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Occupancy }
        val availStats = occupancyTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Availability }

        val requesters = requesters.queueLengthTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Requesters }
        val claimers = claimers.queueLengthTimeline.statsData().asList().asDataFrame()
            .addColumn("Metric") { ResourceMetric.Claimers }

        var statsDF = bindRows(capStats, claimStats, availStats, occStats, requesters, claimers)
        statsDF = statsDF.rename("timestamp" to "start")
        statsDF = statsDF.addColumn("end") { it["start"] + it["duration"] }
        statsDF = statsDF.addColumn("resource") { this@timeline }

        // convert to tick-time
        statsDF = statsDF.addColumn("start") { expr -> expr["start"].map<Double> { TickTime(it) } }
        statsDF = statsDF.addColumn("end") { expr -> expr["end"].map<Double> { TickTime(it) } }

        // optionally add walltimes
//        if (env.tickTransform != null) {
//            statsDF = statsDF.addColumn("start_wt") { it["start"].map<TickTime> { env.asWallTime(it) } }
//            statsDF = statsDF.addColumn("end_wt") { it["end"].map<TickTime> { env.asWallTime(it) } }
//        }

        val records = statsDF.rowsAs<ResourceTimelineSegment>().toList()

//        statsDF.print()
//        statsDF.schema()

        // we also resample with a common time axis using
        // val time = (capStats.keys + claimStats.keys + requesters.keys + claimers.keys).toList().sorted()
//        listOf(1,2,3).map{         capacityTimeline[it] }

        return records
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

    val capacity = resource.capacityTimeline.statistics(false)
    val availableQuantity = resource.availabilityTimeline.statistics(false)
    val claimedQuantity = resource.claimedTimeline.statistics(false)
    val occupancy = resource.occupancyTimeline.statistics(false)


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



