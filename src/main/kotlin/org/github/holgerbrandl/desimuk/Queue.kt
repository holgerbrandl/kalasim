package org.github.holgerbrandl.desimuk

import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import org.koin.core.KoinComponent
import java.util.*

data class CQElement<T>(val t: T, val enterTime: Double)


class ComponentQueue<T : Component>(name:String? = null, val q: Queue<CQElement<T>> = LinkedList()) :
    KoinComponent {

    val size: Int
    get()=q.size

    //    val ass = AggregateSummaryStatistics()
    val lengthOfStayStats = SummaryStatistics()
    val queueLengthStats = Frequency()

    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    init{
        this.name = nameOrDefault(name)

        env.printTrace("create ${this.name}")
    }

    fun add(element: T): Boolean = q.add(CQElement(element, env.now))

    fun poll(): T {
        val (element, enterTime) = q.poll()

        lengthOfStayStats.addValue(enterTime)
        queueLengthStats.addValue(q.size)

        return element
    }

    fun isEmpty() = size==0

    fun isNotEmpty()= !isEmpty()

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
