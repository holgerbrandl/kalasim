package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.jetbrains.kotlinx.dataframe.api.*
import org.kalasim.analysis.*
import org.kalasim.analysis.snapshot.ResourceSnapshot
import org.kalasim.misc.*
import org.kalasim.monitors.*
import org.kalasim.monitors.copy
import org.koin.core.Koin
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration

// TODO Analyze we we support the same preemptible contract as simmer (Ucar2019, p11) (in particular restart)


/** A policy that defines in what order requests are honored. See [user manual](https://www.kalasim.org/resource/#request-honor-policies). */
sealed class RequestHonorPolicy {
    object StrictFCFS : RequestHonorPolicy()
    object RelaxedFCFS : RequestHonorPolicy()
    object SQF : RequestHonorPolicy()
    object RANDOM : RequestHonorPolicy()

    @Suppress("MemberVisibilityCanBePrivate")
    class WeightedFCFS(
        val alpha: Number,
        val capTimeDiffAt: Duration? = null,
        val capQuantityAt: Number? = null
    ) : RequestHonorPolicy() {

        /** Calculate weighted score of quantity and ticks sind request. Requests resulting in large values will be honored first. */
        fun computeRequestWeight(timeSinceRequest: Duration, requestQuantity: Double): Double {
            require(capTimeDiffAt == null || capTimeDiffAt.isPositive())
            require(capQuantityAt == null || capQuantityAt.toDouble() > 0)

            val timeDiffTrimmed = trimDurationOptional(timeSinceRequest, capTimeDiffAt)
            val quantityTrimmed = trimOptional(requestQuantity, capQuantityAt?.toDouble())

            //TODO maybe we need a duration unit or tick-transform to do the mapping
            return (alpha.toDouble() * timeDiffTrimmed.inWholeMinutes) / quantityTrimmed
        }

        private fun trimOptional(value: Double, maxValue: Double?) =
            if(maxValue != null && value > maxValue) maxValue else value

        private fun trimDurationOptional(value: Duration, maxValue: Duration?) =
            if(maxValue != null && value > maxValue) maxValue else value

//        init {
//            require(alpha in 0.0..1.0) { "alpha must be between 0 and 1 " }
//        }
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

    /** Indicates the current level of the resource. Technically is a synonym for `capacity - claimed` */
    val level
        get() = capacity - claimed


    /** Indicates if depletable resource is at full capacity. */
    val isFull: Boolean
        get() = level == capacity

    /** Indicates if depletable resource is depleted (level==0) */
    val isDepleted
        get() = level == 0.0


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


    override fun canComponentHonorQuantity(component: Component, quantity: Double) = when(honorPolicy) {
        RequestHonorPolicy.RANDOM, RequestHonorPolicy.RelaxedFCFS, is RequestHonorPolicy.WeightedFCFS -> {
            canHonorQuantity(quantity)
        }
        RequestHonorPolicy.SQF, RequestHonorPolicy.StrictFCFS -> {
            // note: SQF looks the same as StrictFCFS, but the queue comparator is different
//            (quantity < 0 || requesters.q.peek().component == component) && canHonorQuantity(quantity)
            (quantity <= 0 || (requesters.q as PriorityQueue).sortedIterator()
                .filter { it.component.requests[this]!!.quantity > 0 }
                .firstOrNull()?.component == component) && canHonorQuantity(
                quantity
            )
        }
    }
}

class SQFComparator(val resource: Resource) : Comparator<CQElement<Component>> {
    override fun compare(o1: CQElement<Component>, o2: CQElement<Component>): Int = compareValuesBy(o1, o2,
        { it.priority?.value?.times(-1) ?: 0 },
        { it.component.requests[resource]!!.quantity }
    )
}

class RandomComparator(val random: Random) : Comparator<CQElement<Component>> {
    override fun compare(o1: CQElement<Component>, o2: CQElement<Component>): Int = compareValuesBy(o1, o2,
        { it.priority?.value?.times(-1) ?: 0 },
        { random.nextInt() }
    )
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
    @Suppress("LeakingThis")
    val requesters = ComponentQueue(
        "requesters of ${this.name}",
        comparator = when(honorPolicy) {
            RequestHonorPolicy.SQF -> SQFComparator(this)
            RequestHonorPolicy.RANDOM -> RandomComparator(random)
            else -> PriorityFCFSQueueComparator()
        },
        koin = koin
    )

    val claimers = ComponentQueue<Component>("claimers of ${this.name}", koin = koin)

    var capacity = capacity.toDouble()
        set(newCapacity) {
            validateCapacityRange(newCapacity)

            // disabled for now because it essentially prevents reducing capacity of a fully claimed resource
//            if(newCapacity < claimed) {
//                throw CapacityLimitException(this, "can not reduce capacity below current claims", now, newCapacity)
//            }

            val capacityDiff = newCapacity - field
            field = newCapacity

            capacityTimeline.addValue(capacity)
//            updatedDerivedMetrics()

            if(this is DepletableResource) {
                // maintain the fill level of depletable resources
                claimed += capacityDiff
            }

            tryRequest()
        }


    // https://stackoverflow.com/questions/41214452/why-dont-property-initializers-call-a-custom-setter
    /**  Currently claimed quantity. This can't be set directly but via request */
    var claimed
            : Double = 0.0
        internal set(x) {
            val diffQuantity = field - x

            field = x

            // this ugly hak is needed to avoid tracking of initialLevel setting in DR constructor
            if(this is DepletableResource && diffQuantity == 0.0 && requesters.isEmpty()) return

            if(field < EPS)
                field = 0.0

            claimedTimeline.addValue(x)
//            updatedDerivedMetrics()

//            if(this is DepletableResource && )
//            val action = if (diffQuantity > 0) "Released" else "Claimed"
//            log("$action ${diffQuantity.absoluteValue} from '$name'")

            // it would seem natural to tryReqeust here, but since this is not done in case of bumping,
            // we do it manually at the call-site instead
        }


    // todo when would a capcity less than 0?
    val occupancy
            : Double
        get() = if(capacity < 0) 0.0 else claimed / capacity

    // note: called _just_ available and availableQuanity because more readible in many situations doctors.available
    val available
            : Double
        get() = capacity - claimed



    // because Double.MAX_VALUE-1< Double.MAX_VALUE  is wrong on the JVM (because of limited precision)
    // we need to ensure that capacities are in a range where kalasim will function correctly
    private fun validateCapacityRange(capacity: Number) {
        require(capacity.toDouble() >=0 &&  capacity.toDouble() <= Int.MAX_VALUE) {
            "Only capacities in range [0, Int.MAX_VALUE] are supported by kalasim"
        }
    }

    init{
        validateCapacityRange(capacity)
    }

    val capacityTimeline = MetricTimeline("Capacity of ${super.name}", initialValue = capacity.toDouble(), koin = koin)
    val claimedTimeline = MetricTimeline("Claimed quantity of ${this.name}", koin = koin, initialValue = 0.0)

    //    val availabilityTimeline =
//            MetricTimeline("Available quantity of ${this.name}", initialValue = availableQuantity, koin = koin)
//    val occupancyTimeline = MetricTimeline("Occupancy of ${this.name}", koin = koin)
    val availabilityTimeline
        get() = (capacityTimeline - claimedTimeline) //.also { it.name = "Occupancy of ${this.name}"}

    //    val occupancyTimeline = MetricTimeline("Occupancy of ${this.name}", koin = koin)
    val occupancyTimeline
        get() = claimedTimeline / capacityTimeline


    var trackingPolicy
            : ResourceTrackingConfig = ResourceTrackingConfig()
        set(newPolicy) {
            field = newPolicy

            with(newPolicy) {
                capacityTimeline.enabled = trackUtilization
                claimedTimeline.enabled = trackUtilization

//                availabilityTimeline.enabled = trackUtilization
//                occupancyTimeline.enabled = trackUtilization

                requesters.lengthOfStayStatistics.enabled = trackQueueStatistics
                requesters.sizeTimeline.enabled = trackQueueStatistics
                claimers.lengthOfStayStatistics.enabled = trackQueueStatistics
                claimers.sizeTimeline.enabled = trackQueueStatistics
            }
        }

    init {
        @Suppress("LeakingThis")
        trackingPolicy = env.trackingPolicyFactory.getPolicy(this)

        log(trackingPolicy.logCreation) {
            EntityCreatedEvent(
                now,
                env.currentComponent,
                this,
                "capacity=$capacity " + if(depletable) "anonymous" else ""
            )
        }
    }

    internal fun tryRequest() {
        when(honorPolicy) {
            RequestHonorPolicy.RelaxedFCFS -> {
                // Note: This is an insanely expensive operation, as we need to to build another sorted copy of the requesters queue
                (requesters.q as PriorityQueue).sortedIterator()
                    .filter {
                        // needed to avoid recursion errors when this iterator contains no longer valid request objects
                        // without 'org.kalasim.test.DepletableHonorPolicyTest.it should allow using a relaxed FCFS' will fail
                        it.component.requests.containsKey(this)
                    }
                    .filter {
//                            require(it.component.requests.containsKey(this)){ "invalid requester queue state"}
                        canHonorQuantity(it.component.requests[this]!!.quantity)
                    }
                    .takeWhile { it.component.tryRequest() }
                    .count() // actually trigger otherwise lazy sequence
            }
            RequestHonorPolicy.SQF, RequestHonorPolicy.StrictFCFS, RequestHonorPolicy.RANDOM -> {
                // original port from salabim
                while(requesters.q.isNotEmpty()) {
                    //try honor as many requests as possible
                    if(minq > (capacity - claimed + EPS)) {
                        break
                    }

                    if(!requesters.q.peek().component.tryRequest()) {
                        // if we can not honor this request, we must stop here (to respect request priorities)
                        break
                    }
                }
            }
            is RequestHonorPolicy.WeightedFCFS -> {
                // Note: This is an insanely expensive operation, as we need to resort the requesters queue
                val sortedWith = requesters.q.toList()
                    .sortedWith { o1, o2 ->
                        compareValuesBy(
                            o1, o2,
                            { it.priority?.value?.times(-1) ?: 0 },
                            {
                                val requestQuantity = it.component.requests[this]!!.quantity
                                val timeSinceRequest = now - it.enterTime

                                -1 * honorPolicy.computeRequestWeight(timeSinceRequest, requestQuantity)
                            },
//                                { it.enterTime }
                        )
                    }
                @Suppress("ConvertCallChainIntoSequence")
                sortedWith
                    .filter { canHonorQuantity(it.component.requests[this]!!.quantity) }
                    .takeWhile { it.component.tryRequest() }
                    .count() // actually trigger otherwise lazy sequence
            }
        }
    }

    internal open fun canComponentHonorQuantity(component: Component, quantity: Double) = when(honorPolicy) {
        RequestHonorPolicy.RANDOM, RequestHonorPolicy.RelaxedFCFS, is RequestHonorPolicy.WeightedFCFS -> {
            canHonorQuantity(quantity)
        }
        RequestHonorPolicy.SQF, RequestHonorPolicy.StrictFCFS -> {
            // note: it's looks the same as StrictFCFS, but the queue comparator is different
            requesters.q.peek().component == component && canHonorQuantity(quantity)
        }
    }


    internal fun canHonorQuantity(quantity: Double): Boolean {
        if(quantity > 0 && quantity > capacity - claimed + EPS) {
            return false
        } else if(-quantity > claimed + EPS) {
            return false
        }

        return true
    }


    internal fun removeRequester(component: Component) {
        requesters.remove(component)
        if(requesters.isEmpty()) minq = Double.MAX_VALUE
    }

    /** Prints a summary of statistics of a resource. */
    fun printStatistics() = println(statistics.toString())

    override val snapshot
        get() = ResourceSnapshot(this)

    val statistics
        get() = ResourceStatistics(this)


    val activities
            : List<ResourceActivityEvent> = mutableListOf()
//    val timeline = env.eventCollector<RequestScopeEvent>()

}

internal fun <E> PriorityQueue<E>.sortedIterator() = sequence {
    val pqCopy = PriorityQueue(this@sortedIterator)

    while(pqCopy.isNotEmpty()) {
        yield(pqCopy.poll())
    }
}

//
// Resource timeline for streamlined analytics
//

enum class ResourceMetric { Capacity, Claimed, Requesters, Claimers, Occupancy, Availability }
data class fo(
    val duration: Double?,
    val start: SimTime,
    val value: Double,
    val metric: ResourceMetric,
    val end: SimTime?,
    val resource: Any
)

data class ResourceTimelineSegment(
    val resource: Resource,
    val start: SimTime,
    val end: SimTime?,
    val duration: Duration?,
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

//    val startWT = resource.env.asWtOptional(start)

//    @Suppress("unused")
//    val endWT = end?.let { resource.env.asWtOptional(it) }
}


val Resource.timeline: List<ResourceTimelineSegment>
    get() {
        val capStats = capacityTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Capacity }
        val claimStats = claimedTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Claimed }
        val occStats = occupancyTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Occupancy }
        val availStats = occupancyTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Availability }

        val requesters = requesters.sizeTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Requesters }.convert("value").to<Double>()

        val claimers = claimers.sizeTimeline.statsData().asList().toDataFrame()
            .add("metric") { ResourceMetric.Claimers }
            .convert("value").to<Double>()

        var statsDF = listOf(capStats, claimStats, availStats, occStats, requesters, claimers).concat()
        statsDF = statsDF.rename("timestamp" to "start")
        statsDF = statsDF.add("end") {
            "start"<SimTime>() + ("duration"<Duration?>() ?: Duration.ZERO)
        }
        statsDF = statsDF.add("resource") { this@timeline }

        // convert to tick-time
//        statsDF = statsDF.add("start") { expr -> expr["start"].map<Double> { TickTime(it) } }
        statsDF = statsDF.convert("end").with { it }

        // optionally add walltimes
//        if (env.tickTransform != null) {
//            statsDF = statsDF.add("start_wt") { it["start"].map<TickTime> { env.toWallTime(it) } }
//            statsDF = statsDF.add("end_wt") { it["end"].map<TickTime> { env.toWallTime(it) } }
//        }

        return statsDF.toListOf<ResourceTimelineSegment>()
    }

@Suppress("MemberVisibilityCanBePrivate")
class ResourceStatistics(resource: Resource) : Jsonable() {

    val name = resource.name
    val timestamp = resource.env.now

    val requesters = resource.requesters.statistics
    val claimers = resource.claimers.statistics

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



