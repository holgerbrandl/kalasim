package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.misc.JSON_INDENT
import org.kalasim.misc.Jsonable
import org.kalasim.misc.printThis
import org.kalasim.monitors.NumericLevelMonitor
import org.kalasim.monitors.NumericLevelMonitorStats
import org.kalasim.monitors.NumericStatisticMonitor
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import java.util.*


//TODO add opt-out for queue monitoring

/**
 * Instrumented list with just weak insert order ordering that can be consumed by `poll`. Typically preferred over
 *  `ComponentQueue` if more advanced queue selection policies are needed (e.g. multi-server/service/tool dispatching).
 */
class ComponentList<C>(
    name: String? = null,
    val list: MutableList<C> = LinkedList<C>(),
    private val trackStayStatistics: Boolean = true,
    koin: Koin = GlobalContext.get()
) : SimulationEntity(name, koin), MutableList<C> by list {

    //    val ass = AggregateSummaryStatistics()
    val queueLengthMonitor = NumericLevelMonitor("Length of ${this.name}", koin = koin)
    val lengthOfStayMonitor = NumericStatisticMonitor("Length of stay in ${this.name}", koin = koin)

    internal val stayTracker = mutableMapOf<C, TickTime>()

    override fun add(element: C): Boolean {
        list.add(element)

        changeListeners.forEach { it.added(element) }

        queueLengthMonitor.addValue(size)

        if (trackStayStatistics) {
            stayTracker[element] = env.now
        }

        return true
    }

    override fun remove(element: C): Boolean {
        val removed = list.remove(element)
//        require(remove) { "remove element not in collection" }

        if (removed) {
            changeListeners.forEach { it.removed(element) }

            if (trackStayStatistics) {
                lengthOfStayMonitor.addValue((env.now - stayTracker[element]!!))
            }

            queueLengthMonitor.addValue(size)
        }

        return removed
    }


    fun printStats() = stats.print()

    // todo refactor that out or remove entirely
    fun printHistogram() {
        if (lengthOfStayMonitor.values.size < 2) {
            println("Skipping histogram of '$name' because of to few data")
        } else {
            lengthOfStayMonitor.printHistogram()
            queueLengthMonitor.printHistogram()
        }
    }

    private val changeListeners = mutableListOf<ComponentListChangeListener<C>>()

    fun addChangeListener(changeListener: ComponentListChangeListener<C>): ComponentListChangeListener<C> {
        changeListeners.add(changeListener); return changeListener
    }

    fun removeChangeListener(changeListener: ComponentListChangeListener<C>) = changeListeners.remove(changeListener)

    fun poll(): C? = firstOrNull().also { remove(it) }


    val stats: ComponentListStatistics
        get() = ComponentListStatistics(this)

    override val info: Jsonable
        get() = ComponentListInfo(this)
}

class ComponentListInfo<T>(cq: ComponentList<T>) : Jsonable() {

    data class Entry(val component: String, val enterTime: TickTime?)

    val name = cq.name
    val timestamp = cq.env.now
    val queue = cq.map { Entry(it.toString(), cq.stayTracker[it]) }.toList()
}

//todo this duplicates the impl in ComponentQueue
@Suppress("MemberVisibilityCanBePrivate")
class ComponentListStatistics(cq: ComponentList<*>) {

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


open class ComponentListChangeListener<C> {
    open fun added(component: C) {}
    open fun removed(component: C) {}
    open fun polled(component: C) {}
}