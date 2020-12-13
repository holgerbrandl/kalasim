package org.kalasim

import com.systema.analytics.es.misc.json
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.util.Precision
import org.json.JSONObject
import org.kalasim.misc.JSON_INDENT
import org.kalasim.misc.Jsonable
import org.kalasim.misc.printThis
import org.koin.core.component.KoinComponent
import java.util.*

data class CQElement<C : Component>(val component: C, val enterTime: Double, val priority: Int? = null)

//TODO add opt-out for queue monitoring

class ComponentQueue<C : Component>(
    name: String? = null,
//    val q: Queue<CQElement<T>> = LinkedList()
    val q: Queue<CQElement<C>> = PriorityQueue { o1, o2 -> compareValuesBy(o1, o2, { it.priority }) }
) : KoinComponent, SimulationEntity(name) {

    val size: Int
        get() = q.size

    //    val ass = AggregateSummaryStatistics()
    val queueLengthMonitor = NumericLevelMonitor("Length of ${this.name}")
    val lengthOfStayMonitor = NumericStatisticMonitor("Length of stay in ${this.name}")

    fun add(component: C, priority: Int? = null): Boolean {
        printTrace(component, "entering $name")

        val added = q.add(CQElement(component, env.now, priority))

        queueLengthMonitor.addValue(q.size.toDouble())

        return added
    }

    fun poll(): C {
        val cqe = q.poll()

        updateExitStats(cqe)
        printTrace(cqe.component, "leaving $name")

        return cqe.component
    }

    fun remove(component: C): C {
        val cqe = q.first { it.component == component }
        q.remove(cqe)

        updateExitStats(cqe)

        printTrace(cqe.component, "removed from $name")

        return cqe.component
    }

    private fun updateExitStats(cqe: CQElement<C>) {
        val (_, enterTime) = cqe

        lengthOfStayMonitor.addValue(env.now - enterTime)
        queueLengthMonitor.addValue(q.size.toDouble())
    }

    fun contains(c: C): Boolean = q.any { it.component == c }


    fun isEmpty() = size == 0

    fun isNotEmpty() = !isEmpty()

    fun printStats() = stats.print()

    /** Makes the argument leaving this queue. */
    fun leave(c: C) {
        printTrace(c, "leaving $name")
        q.find { it.component == c }?.let { q.remove(it) }
    }

    val stats: QueueStatistics
        get() = QueueStatistics(this)

    override val info: Jsonable
        get() = QueueInfo(this)
}


class QueueInfo(cq: ComponentQueue<*>): Jsonable() {

    data class Entry(val component: String, val enterTime: Double, val priority: Int?)

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

@Suppress("MemberVisibilityCanBePrivate")
class QueueStatistics(cq: ComponentQueue<*>) {

    val name = cq.name
    val timestamp = cq.env.now

    val lengthStats = cq.queueLengthMonitor.statistics(false)
    val lengthStatsExclZeros = NumericLevelMonitorStats(cq.queueLengthMonitor, excludeZeros = true)

    val lengthOfStayStats = cq.lengthOfStayMonitor.statistics()
    val lengthOfStayStatsExclZeros = cq.lengthOfStayMonitor.statistics(excludeZeros = true)

    // Partial support for weighted percentiles was added in https://github.com/apache/commons-math/tree/fe29577cdbcf8d321a0595b3ef7809c8a3ce0166
    // Update once released, use jitpack or publish manually
//    val nintyfivePercentile = Percentile(0.95).setData()evaluate()


    fun toJson() = json {
        "name" to name
        "timestamp" to timestamp
        "type" to this@QueueStatistics.javaClass.simpleName //"queue statistics"

        "length_of_stay" to {
            "all" to lengthOfStayStats.toJson()
            "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
        }

        "queue_length" to {
            "all" to lengthStats.toJson()
            "excl_zeros" to lengthStatsExclZeros.toJson()
        }
    }

    fun print() = toJson().toString(JSON_INDENT).printThis()
}

fun StatisticalSummary.toJson(): JSONObject {
    return json {
        "entries" to n
        "mean" to mean.roundAny().nanAsNull()
        "standard_deviation" to standardDeviation.roundAny().nanAsNull()

        if (this@toJson is DescriptiveStatistics) {
            "median" to standardDeviation.roundAny().nanAsNull()
            "ninty_pct_quantile" to getPercentile(90.0).nanAsNull()
            "nintyfive_pct_quantile" to getPercentile(95.0).nanAsNull()
        }
    }
}

internal fun Double?.nanAsNull(): Double? = if (this != null && isNaN()) null else this

//private fun DoubleArray.standardDeviation(): Double = StandardDeviation(false).evaluate(this)

internal fun Double?.roundAny(n: Int = 3) = if (this == null) this else Precision.round(this, n)