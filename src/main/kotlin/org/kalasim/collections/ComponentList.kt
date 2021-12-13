@file:Suppress("PackageDirectoryMismatch")
package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.misc.*
import org.kalasim.misc.printThis
import org.kalasim.monitors.MetricTimeline
import org.kalasim.monitors.MetricTimelineStats
import org.kalasim.monitors.NumericStatisticMonitor
import org.koin.core.Koin
import java.util.*


//TODO add opt-out for queue monitoring

/**
 * Instrumented list with just weak insert order ordering that can be consumed by `poll`. Typically preferred over
 *  `ComponentQueue` if more advanced queue selection policies are needed (e.g. multi-server/service/tool dispatching).
 */
class ComponentList<C>(
    name: String? = null,
    capacity: Int =Int.MAX_VALUE,
    private val list: MutableList<C> = LinkedList<C>(),
    koin: Koin = DependencyContext.get()
) : ComponentCollection<C>(name, capacity, koin), MutableList<C> by list {

    internal val stayTracker = mutableMapOf<C, TickTime>()

    init {
        trackingPolicy = env.trackingPolicyFactory.getPolicy(this)
    }

    override fun add(element: C): Boolean {
        checkCapacity()

        list.add(element)

        changeListeners.forEach { it.added(element) }

        queueLengthTimeline.addValue(size)

        stayTracker[element] = env.now

        return true
    }

    override fun remove(element: C): Boolean {
        val removed = list.remove(element)
//        require(remove) { "remove element not in collection" }

        if (removed) {
            changeListeners.forEach { it.removed(element) }

            val insertTime = stayTracker.remove(element)!!
            lengthOfStayTimeline.addValue((env.now - insertTime))

            queueLengthTimeline.addValue(size)
        }

        return removed
    }

    fun printStats() = stats.print()

    fun poll(): C? = firstOrNull().also { remove(it) }

    val stats: ComponentListStatistics
        get() = ComponentListStatistics(this)

    override val info: Jsonable
        get() = ComponentListInfo(this)
}

class ComponentListInfo<T>(cl: ComponentList<T>) : Jsonable() {

    data class Entry(val component: String, val enterTime: TickTime?)

    val name = cl.name
    val timestamp = cl.env.now
    val queue = cl.map { Entry(it.toString(), cl.stayTracker[it]) }.toList()
}

//todo this duplicates the impl in ComponentQueue
@Suppress("MemberVisibilityCanBePrivate")
class ComponentListStatistics(cq: ComponentList<*>) {

    val name = cq.name
    val timestamp = cq.env.now

    val lengthStats = cq.queueLengthTimeline.statistics(false)
    val lengthStatsExclZeros = MetricTimelineStats(cq.queueLengthTimeline, excludeZeros = true)

    val lengthOfStayStats = cq.lengthOfStayTimeline.statistics()
    val lengthOfStayStatsExclZeros = cq.lengthOfStayTimeline.statistics(excludeZeros = true)

    // Partial support for weighted percentiles was added in https://github.com/apache/commons-math/tree/fe29577cdbcf8d321a0595b3ef7809c8a3ce0166
    // Update once released, use jitpack or publish manually
//    val ninetyfivePercentile = Percentile(0.95).setData()evaluate()


    fun toJson() = json {
        "name" to name
        "timestamp" to timestamp.value
        "type" to this@ComponentListStatistics.javaClass.simpleName //"queue statistics"

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

