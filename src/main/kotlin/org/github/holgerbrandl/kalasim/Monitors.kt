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

}


/**
 * Frequency tally levels irrespective of current (simulation) time.
 *
 * @sample org.github.holgerbrandl.kalasim.examples.DokkaExamplesKt.freqLevelDemo
 */
open class FrequencyMonitor<T : Comparable<T>>(name: String? = null) : Monitor<T>(name) {
    val frequencies = Frequency()

    open fun addValue(value: Comparable<T>) {
        frequencies.addValue(value)
    }

    override fun reset() = frequencies.clear()

    fun printHistogram() {
        println(name)
        println("----")
        println("# Records: ${frequencies.getSumFreq()}")
        println()
        frequencies.valuesIterator().asSequence().map {
            println("${it}\t${frequencies.getPct(it)}\t${frequencies.getCount(it)}")
        }
    }

    open fun getPct(value: T): Double = frequencies.getPct(value)
}

/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 *
 * @sample org.github.holgerbrandl.kalasim.examples.DokkaExamplesKt.freqLevelDemo
 */
class FrequencyLevelMonitor<T : Comparable<T>>(name: String? = null) : FrequencyMonitor<T>(name) {
    private val timestamps = listOf<Double>().toMutableList()
    private val values = listOf<Comparable<T>>().toMutableList()

    override fun addValue(value: Comparable<T>) {
        timestamps.add(env.now)
        values.add(value)
//        super.addValue(value)
    }

    override fun getPct(value: T): Double {
        val durations = timestamps.toMutableList()
            .apply { add(env.now) }.zipWithNext { first, second -> second - first }
            .toDoubleArray()

        val freqHist = durations
            .zip(values)
            .groupBy { it.second }
            .mapValues { (_, values) ->
                values.map { it.first }.sum()
            }

        val total =  freqHist.values.sum()

        return (freqHist[value] ?: error("Invalid or non-observed state"))/total
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

    fun printStats() = sumStats.run {
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
        val durations =
            timestamps.toMutableList().apply { add(env.now) }.zipWithNext { first, second -> second - first }
                .toDoubleArray()

        return Mean().evaluate(sumStats.values, durations)
    }
}


fun main() {
//    DiscreteStatisticMonitor<String>().addValue("sdf")
//    DiscreteLevelMonitor<String>().addValue("sdf")
    Environment()

}