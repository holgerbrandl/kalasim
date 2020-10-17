package org.github.holgerbrandl.kalasim

import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import org.koin.core.KoinComponent
import java.util.*

data class CQElement<T : Component>(val t: T, val enterTime: Double)

class ComponentQueue<T : Component>(name: String? = null, val q: Queue<CQElement<T>> = LinkedList()) :
    KoinComponent, SimulationEntity(name) {

    val size: Int
        get() = q.size

    //    val ass = AggregateSummaryStatistics()
    val lengthOfStayStats = SummaryStatistics()
    val queueLengthStats = Frequency()


    fun add(element: T): Boolean {
        env.printTrace(element, "entering " + name)

        queueLengthStats.addValue(q.size)

        return q.add(CQElement(element, env.now))
    }

    fun poll(): T {
        val (element, enterTime) = q.poll()

        env.printTrace(element, "leaving " + name)

        lengthOfStayStats.addValue(enterTime)
        queueLengthStats.addValue(q.size)

        return element
    }

    fun remove(elem:T): T {
        val (component, enterTime) = q.first { it.t == elem  }

        env.printTrace( "leaving " + name)

        lengthOfStayStats.addValue(enterTime)
        queueLengthStats.addValue(q.size)

        return component
    }

    fun isEmpty() = size == 0

    fun isNotEmpty() = !isEmpty()

    fun printStats() = stats.print()

    val stats: QueueStatistics
        get() = QueueStatistics(queueLengthStats, lengthOfStayStats)
}

class QueueStatistics(val queueLengthStats: Frequency, val lengthOfStayStats: SummaryStatistics) {
    fun print() {
        println("Queue Length:")
        println(queueLengthStats)

        println("Length of Stay:")
        println(lengthOfStayStats)
    }

//    todo serialize to json etc

    // add listener for live streaming
}


abstract class SimulationEntity(name: String?) : KoinComponent {
    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    init {
        this.name = nameOrDefault(name)
        env.printTrace("create ${this.name}")
    }
}
