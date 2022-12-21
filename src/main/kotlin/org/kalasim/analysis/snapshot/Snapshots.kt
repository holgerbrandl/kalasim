package org.kalasim.analysis.snapshot

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.json.JSONObject
import org.kalasim.*
import org.kalasim.misc.*
import org.kalasim.monitors.FrequencyTable
import org.kalasim.monitors.MetricTimeline
import kotlin.math.sqrt

/** A representation/snapshot of an entities current state. */
interface EntitySnapshot : WithJson


/** Captures the current state of a `Component`*/
@Suppress("unused")
open class ComponentSnapshot(component: Component) : AutoJson(), EntitySnapshot {
    val name = component.name
    val creationTime: TickTime = component.creationTime
    val now = component.now
    val status = component.componentState
    val scheduledTime = component.scheduledTime

    val claims = component.claims.map { it.key.name to it.value.quantity }.toMap()
    val requests = component.requests.map { it.key.name to it.value.quantity }.toMap()
}



// todo add more context details here
class ComponentGeneratorSnapshot<T>(cg: ComponentGenerator<T>) : ComponentSnapshot(cg)



/** Captures the current state of a `State`*/
//@Serializable
data class StateSnapshot(val time: TickTime, val name: String, val value: String, val waiters: List<String>) :
    AutoJson(), EntitySnapshot {
//    override fun toString(): String {
//        return Json.encodeToString(this)
//    }
}



@Suppress("unused")
class ResourceSnapshot(resource: Resource) : AutoJson(), EntitySnapshot {
    val name: String = resource.name
    val now = resource.now
    val creationTime = resource.creationTime

    val claimedQuantity = resource.claimed
    val capacity = resource.capacity

    // use a dedicated type here to see null prios in json
    val claimedBy = resource.claimers.q.toList().map { it.component.name to it.priority }

    data class ReqComp(val component: String, val quantity: Double?)

    val requestedBy = resource.requesters.q.toList().map {
        ReqComp(it.component.name, it.component.requests[resource]?.quantity)
    }
}



class ComponentListSnapshot<T>(cl: ComponentList<T>) : AutoJson(), EntitySnapshot {

    data class Entry(val component: String, val enterTime: TickTime?)

    val name = cl.name
    val timestamp = cl.env.now
    val queue = cl.map { Entry(it.toString(), cl.stayTracker[it]) }.toList()
}


class QueueSnapshot(cq: ComponentQueue<*>) : AutoJson(), EntitySnapshot {

    data class Entry(val component: String, val enterTime: TickTime, val priority: Priority?)

    val name = cq.name
    val timestamp = cq.env.now
    val queue = cq.q.map { Entry(it.component.toString(), it.enterTime, it.priority) }.toList()

    // salabim example
//    Queue 0x2522b637580
//    name=waitingline
//    component(s):
//    customer.4995        enter_time 49978.472 priority=0
//    customer.4996        enter_time 49991.298 priority=0
}


class FrequencyTableSnapshot<T>(internal val counts: FrequencyTable<T>) : Jsonable(), EntitySnapshot {
    override fun toJson(): JSONObject = json {
        counts.forEach { it.key to counts[it.key]!!.toLong() }
    }
}


class StatisticalSummarySnapshot(internal val ss: StatisticalSummary) : StatisticalSummary by ss, Jsonable(),
    EntitySnapshot {
    override fun toJson(): JSONObject = ss.toJson()
}



//
// Statistical Summary
//

class MetricTimelineSnapshot<V : Number>(nlm: MetricTimeline<V>, excludeZeros: Boolean = false) : Jsonable(),
    EntitySnapshot {
    val duration: Double

    val mean: Double?
    val standardDeviation: Double?

    val min: Double?
    val max: Double?

    internal val data = nlm.statsData(excludeZeros)

//    val median :Double = TODO()
//    val ninetyfivePercentile :Double = TODO()
//    val ninetyninePercentile :Double = TODO()

    init {
        val doubleValues = data.values.map { it.toDouble() }.toDoubleArray()

        min = doubleValues.minOrNull()
        max = doubleValues.maxOrNull()

        if(data.durations.any { it != 0.0 }) {
            val durationsArray = data.durations.toDoubleArray()
            mean = Mean().evaluate(doubleValues, durationsArray)
            standardDeviation = sqrt(Variance().evaluate(doubleValues, durationsArray))
//            val median = Median().evaluate(data.values.toDoubleArray(), data.durations) // not supported by commons3
        } else {
            // this happens if all there is in total no duration associated once 0s are removed
            mean = null
            standardDeviation = null
        }
        // weights not supported
        // mean = Median().evaluate(data.values.toDoubleArray(), data.timepoints.toDoubleArray())

        duration = data.durations.sum()
    }

    override fun toJson() = json {
        "duration" to duration
        "mean" to mean?.roundAny()
        "standard_deviation" to standardDeviation?.roundAny().nanAsNull()
        "min" to min?.roundAny()
        "max" to max?.roundAny()
    }
}
