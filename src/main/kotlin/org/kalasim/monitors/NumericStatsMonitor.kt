package org.kalasim.monitors

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.json.JSONObject
import org.kalasim.misc.Jsonable
import org.kalasim.misc.buildHistogram
import org.kalasim.misc.printHistogram
import org.kalasim.misc.printThis
import org.kalasim.toJson
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt

class NumericStatisticMonitor(name: String? = null, koin: Koin = GlobalContext.get()) :
    Monitor<Number>(name, koin), StatisticMonitor<Number> {
    private val sumStats = ifEnabled { DescriptiveStatistics() }

    val values: DoubleArray
        get() = sumStats.values

    override fun addValue(value: Number) {
        sumStats.addValue(value.toDouble())
    }

    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    operator fun inc(): NumericStatisticMonitor {
        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        addValue((roundToInt + 1).toDouble())

        return this
    }

    operator fun dec(): NumericStatisticMonitor {
        val roundToInt = values.last()
        addValue((roundToInt - 1))

        return this
    }



    override fun reset() = sumStats.clear()


//    open fun mean(): Double? = sumStats.mean
//    open fun standardDeviation(): Double? = sumStats.mean

//    fun statistics(): DescriptiveStatistics = DescriptiveStatistics(sumStats.values)

    fun printHistogram(binCount: Int = NUM_HIST_BINS, valueBins: Boolean = false) {
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
        sumStats.buildHistogram(binCount).printHistogram()
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