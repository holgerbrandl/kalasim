@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import org.kalasim.misc.ComponentCollectionTrackingConfig
import org.kalasim.misc.DependencyContext
import org.kalasim.monitors.MetricTimeline
import org.kalasim.monitors.NumericStatisticMonitor
import org.koin.core.Koin


class CapacityLimitException(
    val source: SimulationEntity,
    msg: String,
    val timestamp: TickTime,
    val capacity: Number
) : Exception(msg)


abstract class ComponentCollection<C>(
    name: String? = null,
    capacity: Int = Int.MAX_VALUE,
    koin: Koin = DependencyContext.get(),
) : SimulationEntity(name, koin) {

    abstract val size: Int

    var capacity = capacity
        set(newCapacity) {
            if (newCapacity > size) {
                val msg = "can not reduce capacity to $newCapacity below current collection size of $size"
                throw CapacityLimitException(this, msg, now, newCapacity)
            }

            field = newCapacity

            capacityTimeline.addValue(capacity)
        }


    internal fun checkCapacity() {
        if (size > capacity) {
            throw CapacityLimitException(this, "Can not more items to collection", now, capacity)
        }
    }


    //    val ass = AggregateSummaryStatistics()
    val queueLengthTimeline = MetricTimeline("Length of ${this.name}", koin = koin)
    val lengthOfStayTimeline = NumericStatisticMonitor("Length of stay in ${this.name}", koin = koin)
    val capacityTimeline = MetricTimeline("Capacity of ${this.name}", initialValue = capacity, koin = koin)


    var trackingPolicy: ComponentCollectionTrackingConfig = ComponentCollectionTrackingConfig()
        set(newPolicy) {
            field = newPolicy

            with(newPolicy) {
                queueLengthTimeline.enabled = trackCollectionStatistics
                lengthOfStayTimeline.enabled = trackCollectionStatistics
            }
        }

    init {
        trackingPolicy = env.trackingPolicyFactory.getPolicy(this)
    }

    fun printHistogram() {
        if (lengthOfStayTimeline.values.size < 2) {
            println("Skipping histogram of '$name' because of to few data")
        } else {
            lengthOfStayTimeline.printHistogram()
            queueLengthTimeline.printHistogram()
        }
    }


    protected val changeListeners = mutableListOf<CollectionChangeListener<C>>()

    fun addChangeListener(changeListener: CollectionChangeListener<C>): CollectionChangeListener<C> {
        changeListeners.add(changeListener); return changeListener
    }


    fun removeChangeListener(changeListener: CollectionChangeListener<C>) = changeListeners.remove(changeListener)
}