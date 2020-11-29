package org.kalasim

import com.systema.analytics.es.misc.json
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.kalasim.misc.printHistogram
import org.kalasim.misc.println
import org.koin.core.KoinComponent
import kotlin.math.roundToInt
import kotlin.math.sqrt


// See https://commons.apache.org/proper/commons-math/userguide/stat.html


abstract class Monitor<T>(name: String? = null) : KoinComponent {

    val env by lazy { getKoin().get<Environment>() }

    /** Disable or enable data collection in a monitor. */
    //TODO implement this function
    var enabled: Boolean = true

    var name: String
        private set

    init {
        this.name = nameOrDefault(name)

//        printTrace("create ${this.name}")
    }

    abstract fun reset()

}


interface LevelMonitor<T> {

    /**
     * When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned.
     * */
    operator fun get(time: Double): T
}

/**
 * Frequency tally levels irrespective of current (simulation) time.
 *
 * @sample org.kalasim.misc.DokkaExamplesKt.freqLevelDemo
 */
open class FrequencyMonitor<T>(name: String? = null) : Monitor<T>(name) {
    val frequencies = mutableMapOf<T, Int>()

    open fun addValue(value: T) {
        frequencies.merge(value, 1, Int::plus)
    }

    val total: Int
        get() = frequencies.values.sum()

    override fun reset() = frequencies.clear()

    open fun printHistogram() {
        println(name)
        println("----")
        println("# Records: ${total}")
        println()
        println("value\t%\tcount")
        // todo make as pretty as in https://www.salabim.org/manual/Monitor.html

        frequencies.keys.asSequence().map {
            println("${it}\t${getPct(it)}\t${frequencies[it]}")
        }
    }

    open fun getPct(value: T): Double = frequencies[value]!!.toDouble() / total

}

/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 *
 * @sample org.kalasim.examples.DokkaExamplesKt.freqLevelDemo
 */
class FrequencyLevelMonitor<T>(initialValue: T, name: String? = null) : FrequencyMonitor<T>(name), LevelMonitor<T> {

    private val timestamps = listOf<Double>().toMutableList()
    private val values = listOf<T>().toMutableList()

    init {
        addValue(initialValue)
    }

    override fun addValue(value: T) {
        timestamps.add(env.now)
        values.add(value)
    }

    override fun getPct(value: T): Double {
        val durations = xDuration()

        val freqHist = durations
            .zip(values)
            .groupBy { it.second }
            .mapValues { (_, values) ->
                values.map { it.first }.sum()
            }

        val total = freqHist.values.sum()

        return (freqHist[value] ?: error("Invalid or non-observed state")) / total
    }

    private fun xDuration(): DoubleArray {
        return timestamps.toMutableList()
            .apply { add(env.now) }.zipWithNext { first, second -> second - first }
            .toDoubleArray()
    }


    override fun get(time: Double): T = TODO("Not yet implemented")
}

open class NumericStatisticMonitor(name: String? = null) : Monitor<Number>(name) {
    private val sumStats = DescriptiveStatistics()

    internal val values: DoubleArray
        get() = sumStats.values

    open fun addValue(value: Number) {
        sumStats.addValue(value.toDouble())
    }

    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    fun inc() {
        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        addValue((roundToInt + 1).toDouble())
    }

    override fun reset(): Unit = TODO("Not yet implemented")


    open fun mean(): Double? = sumStats.mean
    open fun standardDeviation(): Double? = sumStats.mean

    fun printHistogram() {
        sumStats.printHistogram(name)
    }


    fun summary(excludeZeros: Boolean = false, rollingStats: Boolean = false): StatisticalSummary {
        require(!rollingStats){TODO()}

//        val stats: StatisticalSummary = if(rollingStats) SummaryStatistics() else DescriptiveStatistics()
        val stats = DescriptiveStatistics()

        if (excludeZeros) {
            values.filter { it > 0 }.forEach {
                stats.addValue(it)
            }
            //        SummaryStatistics().apply { values.filter { it > 0 }.forEach { addValue(it) } }
        } else {
            values.forEach { stats.addValue(it) }
        }

        return stats
    }
}


/**
 * Allows to track a numeric quantity over time.
 *
 * @param initialValue initial value for a level monitor. It is important to set the value correctly. Default: 0
 */
class NumericLevelMonitor(name: String? = null, initialValue: Number = 0) : NumericStatisticMonitor(name),
    LevelMonitor<Number> {
    val timestamps = listOf<Double>().toMutableList()

    init {
        addValue(initialValue)
    }

    override fun addValue(value: Number) {
        timestamps.add(env.now)
        super.addValue(value)
    }

    override fun get(time: Double): Number = timestamps.zip(values.toList()).first { it.first > time }.second

    internal fun valuesUntilNow(excludeZeros: Boolean = false): NLMStatsData {
        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }
            .toDoubleArray()

        return if (excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second > 0 }.unzip()
            val (_, timestampsFilt) = timestamps.zip(valuesLst).filter { it.second > 0 }.unzip()

            NLMStatsData(valFilt, timestampsFilt, durFilt.toDoubleArray())
        } else {
            NLMStatsData(valuesLst, timestamps, durations)
        }
    }
}


internal data class NLMStatsData(val values: List<Double>, val timepoints: List<Double>, val durations: DoubleArray)

class NumericLevelMonitorStats(nlm: NumericLevelMonitor, excludeZeros: Boolean = false) {
    val duration: Double

    val mean: Double?
    val standardDeviation: Double?

    val min: Double?
    val max: Double?

//    val median :Double = TODO()
//    val ninetyfivePercentile :Double = TODO()
//    val ninetyninePercentile :Double = TODO()

    init {
        val data = nlm.valuesUntilNow(excludeZeros)

        min = data.values.minOrNull()
        max = data.values.maxOrNull()

        if (data.durations.any { it != 0.0 }) {
            mean = Mean().evaluate(data.values.toDoubleArray(), data.durations)
            standardDeviation = sqrt(Variance().evaluate(data.values.toDoubleArray(), data.durations))
        } else {
            // this happens if all there is in total no duration associated once 0s are removed
            mean = null
            standardDeviation = null
        }
        // weights not supported
        // mean = Median().evaluate(data.values.toDoubleArray(), data.timepoints.toDoubleArray())

        duration = data.durations.sum()
    }

    fun toJson() = json {
        "duration" to duration
        "mean" to mean.roundAny()
        "standard_deviation" to standardDeviation.roundAny()
        "min" to min.roundAny()
        "max" to max.roundAny()
    }
}
