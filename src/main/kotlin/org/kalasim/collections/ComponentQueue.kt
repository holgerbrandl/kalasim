@file:Suppress("PackageDirectoryMismatch", "DuplicatedCode")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.json.JSONObject
import org.kalasim.analysis.InteractionEvent
import org.kalasim.analysis.snapshot.MetricTimelineSnapshot
import org.kalasim.analysis.snapshot.QueueSnapshot
import org.kalasim.misc.*
import org.koin.core.Koin
import java.util.*

data class CQElement<C>(val component: C, val enterTime: SimTime, val priority: Priority? = null)

//fun <C> PriorityFCFSQueueComparator() = compareBy<CQElement<C>>(
//    { it.priority?.value?.times(-1) ?: 0 },
//    { it.enterTime }
//)

class PriorityFCFSQueueComparator<C> : Comparator<CQElement<C>> {
    override fun compare(o1: CQElement<C>, o2: CQElement<C>): Int =
        compareValuesBy(o1, o2, { it.priority?.value?.times(-1) ?: DEFAULT_QUEUE_PRIORITY }, { it.enterTime })
}


class ComponentQueue<C>(
    name: String? = null,
    val comparator: Comparator<CQElement<C>> = PriorityFCFSQueueComparator(),
    // for queue alternatives see https://docs.oracle.com/javase/tutorial/collections/implementations/queue.html
    val q: Queue<CQElement<C>> = PriorityQueue(comparator),
    capacity: Int = Int.MAX_VALUE,
    envProvider: EnvProvider = DefaultProvider(),
    trackingConfig: ComponentCollectionTrackingConfig = envProvider.getEnv().entityTrackingDefaults.DefaultComponentCollectionConfig,
) : ComponentCollection<C>(name, capacity, envProvider, trackingConfig) {


    /** Length of queue timeline. Internally a simple wrapper around `sizeTimeline`.*/
    val queueLengthTimeline
        get() = sizeTimeline

    override val size: Int
        get() = q.size

    val components
        get() = q.map { it.component }

    fun asSortedList() = q.toList().sortedWith(comparator)


    fun add(component: C, priority: Priority? = null): Boolean {
        checkCapacity()
//        log(component, "Entering $name")

        val added = q.add(CQElement(component, env.now, priority))

        changeListeners.forEach { it.added(component) }

        sizeTimeline.addValue(q.size)

        return added
    }


    fun poll(): C {
        val cqe = q.poll()

        changeListeners.forEach { it.polled(cqe.component) }
        updateExitStats(cqe)


        log(trackingConfig.trackCollectionStatistics) {
            if(cqe.component is Component) {
                InteractionEvent(env.now, env.currentComponent, cqe.component as Component, "Left $name")
            } else {
                InteractionEvent(env.now, env.currentComponent, null, "${cqe.component} left $name")
            }
        }

        return cqe.component
    }

    fun remove(component: C): C {
        val cqe = q.first { it.component == component }
        q.remove(cqe)

        changeListeners.forEach { it.removed(cqe.component) }
        updateExitStats(cqe)

//        log(cqe.component, "Removed from $name")

        return cqe.component
    }


    private fun updateExitStats(cqe: CQElement<C>) {
        val (_, enterTime) = cqe


        lengthOfStayStatistics.addValue(env.asTicks(env.now - enterTime))
        sizeTimeline.addValue(q.size)
    }

    fun contains(c: C): Boolean = q.any { it.component == c }

    fun isEmpty() = size == 0

    fun isNotEmpty() = !isEmpty()

    override fun toString() = snapshot.toJson().toIndentString()


    /** Update queue position of component after property changes. */
    // TODO add test coverage
    @Suppress("unused")
    fun updateOrderOf(c: C) {
        val element = q.find { it.component == c }

        q.remove(element)
        q.add(element)
    }

    val statistics: QueueStatisticsSnapshot
        get() = QueueStatisticsSnapshot(this)

    override val snapshot
        get() = QueueSnapshot(this)
}


@Suppress("MemberVisibilityCanBePrivate")
class QueueStatisticsSnapshot(cq: ComponentQueue<*>) : Jsonable() {

    val name = cq.name
    val timestamp = cq.env.now

    val lengthStats = cq.sizeTimeline.statistics(false)
    val lengthStatsExclZeros = MetricTimelineSnapshot(cq.sizeTimeline, excludeZeros = true)

    val lengthOfStayStats = cq.lengthOfStayStatistics.statistics()
    val lengthOfStayStatsExclZeros = cq.lengthOfStayStatistics.statistics(excludeZeros = true)

    // Partial support for weighted percentiles was added in https://github.com/apache/commons-math/tree/fe29577cdbcf8d321a0595b3ef7809c8a3ce0166
    // Update once released, use jitpack or publish manually
//    val ninetyfivePercentile = Percentile(0.95).setData()evaluate()


    override fun toJson() = json {
        "name" to name
        "timestamp" to timestamp
        "type" to this@QueueStatisticsSnapshot.javaClass.simpleName //"queue statistics"

        "length_of_stay" to {
            "all" to lengthOfStayStats.toJson()
            "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
        }

        "queue_length" to {
            "all" to lengthStats.toJson()
            "excl_zeros" to lengthStatsExclZeros.toJson()
        }
    }
}


fun StatisticalSummary.toJson(): JSONObject {
    return json {
        "entries" to n
        "mean" to mean.roundAny().nanAsNull()
        "standard_deviation" to standardDeviation.roundAny().nanAsNull()

        if(this@toJson is DescriptiveStatistics) {
            "median" to standardDeviation.roundAny().nanAsNull()
            "ninety_pct_quantile" to getPercentile(90.0).roundAny().nanAsNull()
            "ninetyfive_pct_quantile" to getPercentile(95.0).roundAny().nanAsNull()
        }
    }
}

internal fun Double?.nanAsNull(): Double? = if(this != null && isNaN()) null else this

//private fun DoubleArray.standardDeviation(): Double = StandardDeviation(false).evaluate(this)


open class CollectionChangeListener<C> {
    open fun added(component: C) {}
    open fun removed(component: C) {}
    open fun polled(component: C) {}
}