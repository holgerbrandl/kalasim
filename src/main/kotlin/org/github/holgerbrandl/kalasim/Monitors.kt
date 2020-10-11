package org.github.holgerbrandl.kalasim

import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.koin.core.KoinComponent


// See https://commons.apache.org/proper/commons-math/userguide/stat.html


abstract class Monitor<T>(name: String? = null) : KoinComponent {

    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    init {
        this.name = nameOrDefault(name)

        env.printTrace("create ${this.name}")
    }

    abstract fun reset()

    abstract fun printStats()
}


/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 */
open class DiscreteStatisticMonitor<T : Comparable<T>>(name: String? = null) : Monitor<T>(name) {
    val queueLengthStats = Frequency()

    open fun addValue(value: Comparable<T>) {
        queueLengthStats.addValue(value)
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun printStats() {
        println(name)
        println("----")

        queueLengthStats.valuesIterator().asSequence().map {
            println("${it}\t${queueLengthStats.getPct(it)}\t${queueLengthStats.getCount(it)}")
        }
    }
}


class DiscreteLevelMonitor<T : Comparable<T>>(name: String? = null) : DiscreteStatisticMonitor<T>(name) {
    private val durations = DescriptiveStatistics()

    override fun addValue(value: Comparable<T>) {
        super.addValue(value)
        durations.addValue(env.now)
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun printStats() {
        super.printStats()
    }
}

open class NumericStatisticMonitor(name: String? = null) : Monitor<Number>(name) {
    //    val sumStats = SummaryStatistics()
    val sumStats = DescriptiveStatistics()

    open fun addValue(value: Number) {
        sumStats.addValue(value.toDouble())
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun printStats() = sumStats.run {
        println(
            """"
       |name
       |entries\t\t${n}
       |minimum\t\t${min}
       |mean\t\t${mean()}
       |minimum\t\t${min}
       |maximum\t\t${max}
       |"""".trimMargin()
        )
    }

     open fun mean() = sumStats.mean
}

class NumericLevelMonitor(name: String? = null) : NumericStatisticMonitor(name) {
    private val timestamps = listOf<Double>().toMutableList()

    override fun addValue(value: Number) {
        timestamps.add(env.now)
//        if (sumStats.n == 0L){
//            durations.addValue(Double.MAX_VALUE)
//        }else{
//            durations.addValue(env.now - durations.values.last())
//        }

        super.addValue(value)
    }

    override fun mean(): Double {
        val durations = timestamps.toMutableList().apply { add(env.now) }.zipWithNext { first, second -> second - first }.toDoubleArray()

        return Mean().evaluate(sumStats.values, durations)
    }
}


fun main() {
//    DiscreteStatisticMonitor<String>().addValue("sdf")
//    DiscreteLevelMonitor<String>().addValue("sdf")
    Environment()

}