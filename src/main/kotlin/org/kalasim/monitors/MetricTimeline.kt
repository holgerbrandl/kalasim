package org.kalasim.monitors

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.Frequency
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.Variance
import org.kalasim.TickTime
import org.kalasim.asCMPairList
import org.kalasim.misc.Jsonable
import org.kalasim.misc.buildHistogram
import org.kalasim.misc.printHistogram
import org.kalasim.misc.printThis
import org.kalasim.misc.roundAny
import org.koin.core.Koin
import org.kalasim.misc.DependencyContext
import kotlin.math.sqrt

/**
 * Allows to track a numeric quantity over time.
 *
 * @param initialValue initial value for a level timeline. It is important to set the value correctly. Default: 0
 */
class MetricTimeline(name: String? = null, private val initialValue: Number = 0, koin: Koin = DependencyContext.get()) :
    Monitor<Number>(name, koin),
    ValueTimeline<Number> {

    private val timestamps = listOf<Double>().toMutableList()
    private val values = ifEnabled { listOf<Double>().toMutableList() }

    init {
        addValue(initialValue)
    }


    override fun addValue(value: Number) {
        if (!enabled) return

        timestamps.add(env.now.value)
        values.add(value.toDouble())
    }


    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    operator fun inc(): MetricTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt + 1))

        return this
    }

    operator fun dec(): MetricTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt - 1))

        return this
    }

    override fun get(time: Number): Number {
        require(time.toDouble() >= timestamps.first()) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        return timestamps.zip(values.toList()).reversed().first { it.first <= time.toDouble() }.second
    }

    operator fun get(time: TickTime) = get(time.value)

    override fun total(value: Number): Double = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.map { it.second }.sum()
    }

    @Suppress("UNCHECKED_CAST")
    override fun statisticsSummary() = (statsData() as LevelStatsData<Number>).statisticalSummary()

    fun statsData(excludeZeros: Boolean = false): LevelStatsData<Double> {
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now.value
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return if (excludeZeros) {
            val (durFilt, valFilt) = durations.zip(valuesLst).filter { it.second > 0 }.unzip()
            val (_, timestampsFilt) = timestamps.zip(valuesLst).filter { it.second > 0 }.unzip()

            LevelStatsData(valFilt, timestampsFilt, durFilt)
        } else {
            LevelStatsData(valuesLst, timestamps, durations)
        }
    }

    /** Returns the step function of this monitored value along the time axis. */
    override fun stepFun() = statsData().stepFun()

    fun statistics(excludeZeros: Boolean = false) = MetricTimelineStats(this, excludeZeros)

    fun printHistogram(sortByWeight: Boolean = false, binCount: Int = NUM_HIST_BINS, valueBins: Boolean = false) {
        println("Summary of: '${name}'")
        statistics().toJson().printThis()

        println("Histogram of: '${name}'")

        if (valueBins) {
            val freq = Frequency()

            values.forEach { freq.addValue(it) }

            val colData: Map<Double, Double> = statistics().data.run {
                durations.zip(values).groupBy { (_, value) -> value }
                    .map { it.key to it.value.sumOf { it.first } }
            }.toMap()

            // inherently wrong because does not take into account durations
//            val byValueHist = freq
//                .valuesIterator().iterator().asSequence().toList()
//                .map { it to freq.getCount(it) }

            colData.printConsole(sortByWeight = sortByWeight)
        } else {

            // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
            val hist: List<Pair<Double, Double>> = statistics().data.run {
                val aggregatedMonitor: List<Pair<Double, Double>> =
                    durations.zip(values).groupBy { (_, value) -> value }.map { it.key to it.value.sumOf { it.first } }

                aggregatedMonitor
            }

            val stats =
                DescriptiveStatistics(
                    EnumeratedDistribution(hist.asCMPairList()).sample(1000, arrayOf<Double>()).toDoubleArray()
                )

            stats.buildHistogram(binCount).printHistogram()
        }
    }

    override val info: Jsonable
        get() = statistics(false)

    override fun reset(initial: Number) {
        require(enabled){ "resetting a disabled timeline is unlikely to have meaningful semantics"}

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

}

class MetricTimelineStats(nlm: MetricTimeline, excludeZeros: Boolean = false) : Jsonable() {
    val duration: Double

    val mean: Double?
    val standardDeviation: Double?

    val min: Double?
    val max: Double?

    internal val data = nlm.statsData(excludeZeros)

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
