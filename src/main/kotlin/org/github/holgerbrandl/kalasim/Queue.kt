package org.github.holgerbrandl.kalasim

import com.systema.analytics.es.misc.json
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import org.apache.commons.math3.util.Precision
import org.github.holgerbrandl.kalasim.misc.println
import org.koin.core.KoinComponent
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
    val queueLengthMonitor = NumericLevelMonitor("Length of $name")
    val lengthOfStayMonitor = NumericStatisticMonitor("Length of stay in $name")

    fun add(element: C, priority: Int? = null): Boolean {
        printTrace(element, "entering $name")

        val added = q.add(CQElement(element, env.now, priority))

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

        printTrace(cqe.component,"removed from $name")

        return cqe.component
    }

    private fun updateExitStats(cqe: CQElement<C>) {
        val (_, enterTime) = cqe

        lengthOfStayMonitor.addValue(env.now - enterTime)
        queueLengthMonitor.addValue(q.size.toDouble())
    }

    fun contains(t: C): Boolean =  q.any { it.component == t }


    fun isEmpty() = size == 0

    fun isNotEmpty() = !isEmpty()

    fun printStats() = stats.print()

    /** Makes the argument leaving this queue. */
    fun leave(c: C) {
        printTrace(c, "leaving $name")
        q.find {  it.component ==c }?.let { q.remove(it)}
    }

    val stats: QueueStatistics
        get() = QueueStatistics(this)

    override val info: Snapshot
        get() = TODO()
}


@Suppress("MemberVisibilityCanBePrivate")
class QueueStatistics(cq: ComponentQueue<*>) {

    val name = cq.name

    val lengthStats = NumericLevelMonitorStats(cq.queueLengthMonitor)
    val lengthStatsExclZeros = NumericLevelMonitorStats(cq.queueLengthMonitor, excludeZeros = true)

    val lengthOfStayStats = cq.lengthOfStayMonitor.summary()
    val lengthOfStayStatsExclZeros = cq.lengthOfStayMonitor.summary(excludeZeros = true)

    // Partial support for weighted percentiles was added in https://github.com/apache/commons-math/tree/fe29577cdbcf8d321a0595b3ef7809c8a3ce0166
    // Update once released, use jitpack or publish manually
//    val nintyfivePercentile = Percentile(0.95).setData()evaluate()


    fun toJson() = json {
        "type" to "queue statistics"
        "name" to name

        "length_of_stay" to {
            "all" to lengthOfStayStats.toJson()
            "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
        }

        "queue_length" to {
            "all" to lengthStats.toJson()
            "excl_zeros" to lengthStatsExclZeros.toJson()
        }
    }

    fun print() = toJson().toString(3).println()
}

fun SummaryStatistics.toJson(): Any {
    return json {
        "entries" to n
        "mean" to mean.roundAny()
        "standard_deviation" to standardDeviation.roundAny()
        // TODO percentiles
    }
}

//private fun DoubleArray.standardDeviation(): Double = StandardDeviation(false).evaluate(this)

internal fun Double?.roundAny(n: Int = 3) = if (this == null) this else Precision.round(this, n)