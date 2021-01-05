@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_OVERRIDE")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.json.JSONObject
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt


// See https://commons.apache.org/proper/commons-math/userguide/stat.html


abstract class Monitor<T>(
    name: String? = null,
    koin: Koin = GlobalContext.get()
) : SimulationEntity(name, koin) {

    /** Disable or enable data collection in a monitor. */
    var enabled: Boolean = true
        protected set

    fun disable() {
        enabled = false
    }


    fun <T> ifEnabled(query: () -> T): T {
        if (!enabled) {
            throw  IllegalArgumentException("can not query disabled monitor")
        }

        return query()
    }
}


interface StatisticMonitor<T> {

    /** Resets the monitor. This will also reenable it as a side-effect. */
    fun reset()

    fun addValue(value: T)
}

interface LevelMonitor<T> {

    /**
     * When Monitor.get() is called with a time parameter or a direct call with a time parameter, the value at that time will be returned.
     * */
    operator fun get(time: Double): T?


    /** Get the total time for which a monitor was is state `value`*/
    operator fun get(value: T): Double?


    /** Resets the monitor to a new initial at the current simulation clock. This will also reenable it as a side-effect. */
    fun reset(initial: T)

    fun addValue(value: T)
}

/**
 * Frequency tally levels irrespective of current (simulation) time.
 *
 * @sample org.kalasim.misc.DokkaExamplesKt.freqLevelDemo
 */
open class FrequencyMonitor<T>(
    name: String? = null,
    koin: Koin = GlobalContext.get()
) : Monitor<T>(name, koin), StatisticMonitor<T> {

    val frequencies = mutableMapOf<T, Int>()
        get() = ifEnabled { field }

    override fun addValue(value: T) {
        if (!enabled) return

        frequencies.merge(value, 1, Int::plus)
    }

    val total: Int
        get() = frequencies.values.sum()

    override fun reset() = frequencies.clear()

    open fun printHistogram(values: List<T>? = null, sortByWeight: Boolean = false) {
        println("Summary of: '${name}'")
        println("# Records: ${total}")
        println("# Levels: ${frequencies.keys.size}")
        println()

        // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
        println("Histogram of: '${name}'")
        frequencies.mapValues { it.value.toLong() }.toList()
            .printHistogram(values = values, sortByWeight = sortByWeight)
//        frequencies.keys.asSequence().map {
//            println("${it}\t${getPct(it)}\t${frequencies[it]}")
//        }
    }

    open fun getPct(value: T): Double = frequencies[value]!!.toDouble() / total

    override val info: Jsonable
        get() = ImplementMe()
}

/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 *
 * @sample org.kalasim.misc.DokkaExamplesKt.freqLevelDemo
 */
class FrequencyLevelMonitor<T>(
    initialValue: T,
    name: String? = null,
    koin: Koin = GlobalContext.get()
) : Monitor<T>(name, koin), LevelMonitor<T> {

    private val timestamps = listOf<Double>().toMutableList()
    private val values = ifEnabled { listOf<T>().toMutableList() }

    init {
        reset(initialValue)
    }

    override fun reset(initial: T) {
        enabled = true

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

    override fun addValue(value: T) {
        if (!enabled) return

        timestamps.add(env.now)
        values.add(value)
    }

    fun getPct(value: T): Double {
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


    override fun get(time: Double): T? {
        // https://youtrack.jetbrains.com/issue/KT-43776
        val timeIndex = timestamps.withIndex().firstOrNull { it.value >= time }?.index

        return timeIndex?.let { values[it] }
    }

    override fun get(value: T): Double = valuesUntilNow().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.map { it.second }.sum()
    }

    fun printHistogram(values: List<T>? = null, sortByWeight: Boolean = false) {
        println("Summary of: '${name}'")
        println("Duration: ${env.now - timestamps[0]}")
        println("# Levels: ${this.values.distinct().size}")
        println()

        val hist: List<Pair<T, Long>> =
            xDuration().zip(this.values).groupBy { (_, value) -> value }
                .map { it.key to it.value.sumOf { (it.first * 100).roundToLong() } }

//        val ed = EnumeratedDistribution(hist.asCM())
//        repeat(1000){ ed.sample()}.c

        hist.printHistogram(sortByWeight = sortByWeight, values = values)
    }


    fun valuesUntilNow(): LevelStatsData<T> {
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return LevelStatsData(valuesLst, timestamps, durations)
    }


    override val info: Jsonable
        get() = ImplementMe()
}

private val NUM_HIST_BINS = 10

class NumericStatisticMonitor(name: String? = null, koin: Koin = GlobalContext.get()) :
    Monitor<Number>(name, koin) {
    private val sumStats = ifEnabled { DescriptiveStatistics() }

    val values: DoubleArray
        get() = sumStats.values

    fun addValue(value: Number) {
        sumStats.addValue(value.toDouble())
    }

    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    fun inc() {
        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        addValue((roundToInt + 1).toDouble())
    }

    fun reset() = sumStats.clear()


//    open fun mean(): Double? = sumStats.mean
//    open fun standardDeviation(): Double? = sumStats.mean

//    fun statistics(): DescriptiveStatistics = DescriptiveStatistics(sumStats.values)

    fun printHistogram(sortByWeight: Boolean = false, binCount: Int = NUM_HIST_BINS, valueBins: Boolean = false) {
        //    val histJson = JSONArray(GSON.toJson(histogramScaled))

        //    json {
        //        "name" to name
        //        "type" to this@printHistogram.javaClass.simpleName //"queue statistics"
        //        "entries" to n
        //        "mean" to mean
        //        "minimum" to min
        //        "maximum" to max
        //    }.toString(2).println()


//        val histogram = sumStats.buildHistogram()
//        val colWidth = 40.0
//
//        val histogramScaled = histogram.map { (range, value) -> range to colWidth * value / sumStats.n }
        println("Summary of: '${name}'")
        statistics().printThis()

        println("Histogram of: '${name}'")
        sumStats.buildHistogram(binCount).printHistogram(sortByWeight = sortByWeight)
    }


    fun statistics(excludeZeros: Boolean = false, rollingStats: Boolean = false): NumericStatisticMonitorStats {
        require(!rollingStats) { TODO() }

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

        return NumericStatisticMonitorStats(stats)
    }

    fun enable() {
        enabled = true
    }

    override val info: Jsonable
        get() = statistics(false)
}

class NumericStatisticMonitorStats(internal val ss: StatisticalSummary) : StatisticalSummary by ss, Jsonable() {
    override fun toJson(): JSONObject = ss.toJson()
}


/**
 * Allows to track a numeric quantity over time.
 *
 * @param initialValue initial value for a level monitor. It is important to set the value correctly. Default: 0
 */
class NumericLevelMonitor(name: String? = null, initialValue: Number = 0, koin: Koin = GlobalContext.get()) :
    Monitor<Number>(name, koin),
    LevelMonitor<Number> {

    private val timestamps = listOf<Double>().toMutableList()
    private val values = ifEnabled { listOf<Double>().toMutableList() }

    init {
        addValue(initialValue)
    }

    override fun addValue(value: Number) {
        if (!enabled) return

        timestamps.add(env.now)
        values.add(value.toDouble())
    }


    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    fun inc(): NumericLevelMonitor {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt + 1))

        return this
    }

    fun dec(): NumericLevelMonitor {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt - 1))

        return this
    }

    override fun get(time: Double): Number = timestamps.zip(values.toList()).first { it.first > time }.second

    override fun get(value: Number): Double = valuesUntilNow().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.map { it.second }.sum()
    }

    fun valuesUntilNow(excludeZeros: Boolean = false): LevelStatsData<Double> {
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return if (excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second > 0 }.unzip()
            val (_, timestampsFilt) = timestamps.zip(valuesLst).filter { it.second > 0 }.unzip()

            LevelStatsData(valFilt, timestampsFilt, durFilt)
        } else {
            LevelStatsData(valuesLst, timestamps, durations)
        }
    }

    fun statistics(excludeZeros: Boolean = false) = NumericLevelMonitorStats(this, excludeZeros)

    fun printHistogram(sortByWeight: Boolean = false, binCount: Int = NUM_HIST_BINS, valueBins: Boolean = false) {
        println("Summary of: '${name}'")
        statistics().toJson().printThis()

        println("Histogram of: '${name}'")

        if (valueBins) {
            val freq = Frequency()

            values.forEach { freq.addValue(it) }

            val byValueHist: List<Pair<Double, Long>> = statistics().data.run {
                durations.zip(values).groupBy { (_, value) -> value }
                    .map { it.key to it.value.sumOf { it.first }.roundToLong() }
            }


            // inherently wrong because does not take into account durations
//            val byValueHist = freq
//                .valuesIterator().iterator().asSequence().toList()
//                .map { it to freq.getCount(it) }

            byValueHist.printHistogram(sortByWeight = sortByWeight)

        } else {

            // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
            val hist: List<Pair<Double, Double>> = statistics().data.run {
                val aggregatedMonitor: List<Pair<Double, Double>> =
                    durations.zip(values).groupBy { (_, value) -> value }.map { it.key to it.value.sumOf { it.first } }

                aggregatedMonitor
            }

            val stats =
                DescriptiveStatistics(
                    EnumeratedDistribution(hist.asCM()).sample(1000, arrayOf<Double>()).toDoubleArray()
                )

            stats.buildHistogram(binCount).printHistogram(sortByWeight = sortByWeight)
        }
    }

    override val info: Jsonable
        get() = statistics(false)

    override fun reset(initial: Number) {
        enabled = true

        values.clear()
        timestamps.clear()

        addValue(initial)
    }
}


class LevelMonitoredInt(initialValue: Int = 0, name: String? = null, koin: Koin = GlobalContext.get()) {
    var value: Int = initialValue
        set(value) {
            field = value
            monitor.addValue(value)
        }

    val monitor by lazy { NumericLevelMonitor(name, koin = koin) }

    override fun toString(): String = value.toString()
}


//**{todo}** use monitors here and maybe even inject them
//to inject use data class Counter(var value: Int)
//val numBalkedMonitor by lazy { NumericLevelMonitor() }
//var numBalked: Int = 0
//    set(value) {
//        field = value
//        numBalkedMonitor.addValue(value)
//    }


//internal data class NLMStatsData(val values: List<Double>, val timepoints: List<Double>, val durations: DoubleArray) {
//    fun plotData(): List<Pair<Double, Double>> =
//        (this.timepoints + (timepoints.last() + durations.last())).zip(values.toList() + values.last())
//}

data class LevelStatsData<T>(
    val values: List<T>,
    val timepoints: List<Double>,
    val durations: List<Double>
) {
    fun plotData(): List<Pair<Double, T>> =
        (this.timepoints + (timepoints.last() + durations.last())).zip(values.toList() + values.last())
}

class NumericLevelMonitorStats(nlm: NumericLevelMonitor, excludeZeros: Boolean = false) : Jsonable() {
    val duration: Double

    val mean: Double?
    val standardDeviation: Double?

    val min: Double?
    val max: Double?

    internal val data: LevelStatsData<Double> = nlm.valuesUntilNow(excludeZeros)

//    val median :Double = TODO()
//    val ninetyfivePercentile :Double = TODO()
//    val ninetyninePercentile :Double = TODO()

    init {
        min = data.values.minOrNull()
        max = data.values.maxOrNull()

        if (data.durations.any { it != 0.0 }) {
            val durationsArray = data.durations.toDoubleArray()
            mean = Mean().evaluate(data.values.toDoubleArray(), durationsArray)
            standardDeviation = sqrt(Variance().evaluate(data.values.toDoubleArray(), durationsArray))
//            val median = Median().evaluate(data.values.toDoubleArray(), data.durations) // not supported by commons3
        } else {
            // this happens if all there is in total no duration associated once 0s are removed
            mean = null
            standardDeviation = null
        }
        // weights not supported
        // mean = Median().evaluate(data.values.toDoubleArray(), data.timepoints.toDoubleArray())

        duration = data.durations.sum()
    }

    override fun toJson() = json {
        "duration" to duration
        "mean" to mean.roundAny()
        "standard_deviation" to standardDeviation.roundAny()
        "min" to min.roundAny()
        "max" to max.roundAny()
    }
}
