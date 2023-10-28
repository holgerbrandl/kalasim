@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.analysis.snapshot.ComponentListSnapshot
import org.kalasim.analysis.snapshot.MetricTimelineSnapshot
import org.kalasim.misc.*
import org.koin.core.Koin
import java.util.*


/**
 * Instrumented list with just weak insert order ordering that can be consumed by `poll`. Typically preferred over
 *  `ComponentQueue` if more advanced queue selection policies are needed (e.g. multi-server/service/tool dispatching).
 */
class ComponentList<C>(
    name: String? = null,
    capacity: Int = Int.MAX_VALUE,
    private val list: MutableList<C> = LinkedList<C>(),
    koin: Koin = DependencyContext.get()
) : ComponentCollection<C>(name, capacity, koin), MutableList<C> by list {

    internal val stayTracker = mutableMapOf<C, SimTime>()

    init {
        trackingPolicy = env.trackingPolicyFactory.getPolicy(this)
    }

    override fun add(element: C): Boolean {
        checkCapacity()

        list.add(element)

        changeListeners.forEach { it.added(element) }

        sizeTimeline.addValue(size)

        stayTracker[element] = env.now

        return true
    }

    override fun remove(element: C): Boolean {
        val removed = list.remove(element)
//        require(remove) { "remove element not in collection" }

        if (removed) {
            changeListeners.forEach { it.removed(element) }

            val insertTime = stayTracker.remove(element)!!
            lengthOfStayStatistics.addValue(env.asTicks(env.now - insertTime))

            sizeTimeline.addValue(size)
        }

        return removed
    }


    fun poll(): C? = firstOrNull().also { remove(it) }

    val statistics: ComponentListStatistics
        get() = ComponentListStatistics(this)

     override val snapshot
        get() = ComponentListSnapshot(this)
}



//todo this duplicates the impl in ComponentQueue
@Suppress("MemberVisibilityCanBePrivate")
class ComponentListStatistics(cl: ComponentList<*>) : Jsonable(){

    val name = cl.name
    val timestamp = cl.env.now

    val sizeStats = cl.sizeTimeline.statistics(false)
    val sizeStatsExclZeros = MetricTimelineSnapshot(cl.sizeTimeline, excludeZeros = true)

    val lengthOfStayStats = cl.lengthOfStayStatistics.statistics()
    val lengthOfStayStatsExclZeros = cl.lengthOfStayStatistics.statistics(excludeZeros = true)

    // Partial support for weighted percentiles was added in https://github.com/apache/commons-math/tree/fe29577cdbcf8d321a0595b3ef7809c8a3ce0166
    // Update once released, use jitpack or publish manually
//    val ninetyfivePercentile = Percentile(0.95).setData()evaluate()


    override fun toJson() = json {
        "name" to name
        "timestamp" to timestamp
        "type" to this@ComponentListStatistics.javaClass.simpleName //"queue statistics"

        "length_of_stay" to {
            "all" to lengthOfStayStats.toJson()
            "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
        }

        "size" to {
            "all" to sizeStats.toJson()
            "excl_zeros" to sizeStatsExclZeros.toJson()
        }
    }
}

