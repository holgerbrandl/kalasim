package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.util.Precision
import org.json.JSONObject
import org.kalasim.misc.JSON_INDENT
import org.kalasim.misc.Jsonable
import org.kalasim.misc.printThis
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import java.util.*

data class CQElement<C : Component>(val component: C, val enterTime: Double, val priority: Int? = null)

//TODO add opt-out for queue monitoring

class ComponentQueue<C : Component>(
    name: String? = null,
//    val q: Queue<CQElement<T>> = LinkedList()
    val q: Queue<CQElement<C>> = PriorityQueue { o1, o2 -> compareValuesBy(o1, o2, { it.priority }) },
    koin: Koin = GlobalContext.get()
) : SimulationEntity(name, koin) {

    val size: Int
        get() = q.size

    val components
        get() = q.map{it.component}

    //    val ass = AggregateSummaryStatistics()
    val queueLengthMonitor = NumericLevelMonitor("Length of ${this.name}", koin = koin)
    val lengthOfStayMonitor = NumericStatisticMonitor("Length of stay in ${this.name}", koin = koin)

    fun add(component: C, priority: Int? = null): Boolean {
        log(component, "Entering $name")

        val added = q.add(CQElement(component, env.now, priority))

        changeListeners.forEach{ it.added(component)}

        queueLengthMonitor.addValue(q.size.toDouble())

        return added
    }

    fun poll(): C {
        val cqe = q.poll()

        changeListeners.forEach{ it.polled(cqe.component)}
        updateExitStats(cqe)

        log(cqe.component, "Left $name")

        return cqe.component
    }

    fun remove(component: C): C {
        val cqe = q.first { it.component == component }
        q.remove(cqe)

        changeListeners.forEach{ it.removed(cqe.component)}
        updateExitStats(cqe)

        log(cqe.component, "Removed from $name")

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

    fun printHistogram() {
        lengthOfStayMonitor.printHistogram()
        queueLengthMonitor.printHistogram()
    }

    private val changeListeners = mutableListOf<QueueChangeListener<C>>()

    fun addChangeListener(changeListener: QueueChangeListener<C>): QueueChangeListener<C> {
        changeListeners.add(changeListener); return changeListener
    }

    fun removeChangeListener(changeListener: QueueChangeListener<C>) = changeListeners.remove(changeListener)


    val stats: QueueStatistics
        get() = QueueStatistics(this)

    override val info: Jsonable
        get() = QueueInfo(this)
}


class QueueInfo(cq: ComponentQueue<*>) : Jsonable() {

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
            "ninty_pct_quantile" to getPercentile(90.0).roundAny().nanAsNull()
            "nintyfive_pct_quantile" to getPercentile(95.0).roundAny().nanAsNull()
        }
    }
}

internal fun Double?.nanAsNull(): Double? = if (this != null && isNaN()) null else this

//private fun DoubleArray.standardDeviation(): Double = StandardDeviation(false).evaluate(this)

internal fun Double?.roundAny(n: Int = 3) = if (this == null) this else Precision.round(this, n)


open class QueueChangeListener<C>{
    open fun added(component:C){}
    open fun removed(component:C){}
    open fun polled(component:C){}
}