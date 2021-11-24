package org.kalasim.monitors

import org.kalasim.TickTime
import org.kalasim.misc.ImplementMe
import org.kalasim.misc.Jsonable
import org.koin.core.Koin
import org.kalasim.misc.DependencyContext

/**
 * Level monitors tally levels along with the current (simulation) time. e.g. the number of parts a machine is working on.
 *
 * @sample org.kalasim.misc.DokkaExamplesKt.freqLevelDemo
 */
class CategoryTimeline<T>(
    val initialValue: T,
    name: String? = null,
    koin: Koin = DependencyContext.get()
) : Monitor<T>(name, koin), ValueTimeline<T> {

    override var enabled: Boolean = true


    private val timestamps = listOf<Double>().toMutableList()
    private val values = ifEnabled { listOf<T>().toMutableList() }

    init {
        reset(initialValue)
    }


    override fun reset(initial: T) {
        require(enabled){ "resetting a disabled timeline is unlikely to have meaningful semantics"}

        values.clear()
        timestamps.clear()

        addValue(initial)
    }

    override fun addValue(value: T) {
        if (!enabled) return

        timestamps.add(env.now.value)
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
            .apply { add(env.now.value) }.zipWithNext { first, second -> second - first }
            .toDoubleArray()
    }


    override fun get(time: Number): T? {
        require(time.toDouble() >= timestamps.first()) {
            "query time must be greater than timeline start (${timestamps.first()})"
        }

        // https://youtrack.jetbrains.com/issue/KT-43776
        return timestamps.zip(values.toList()).reversed().first { it.first <= time.toDouble() }.second
    }

    operator fun get(time: TickTime) = get(time.value)

    override fun total(value: T): Double = statsData().run {
        // https://youtrack.jetbrains.com/issue/KT-43776
        values.zip(durations).filter { it.first == value }.map { it.second }.sum()
    }

    fun printHistogram(values: List<T>? = null, sortByWeight: Boolean = false) {
        println("Summary of: '${name}'")
        println("Duration: ${env.now - timestamps[0]}")
        println("# Levels: ${this.values.distinct().size}")
        println()

        if(this.values.size <= 1){
            println("Skipping histogram of '$name' because of to few data")
            return
        }

        //        val ed = EnumeratedDistribution(hist.asCM())
//        repeat(1000){ ed.sample()}.c

        summed().printConsole(sortByWeight = sortByWeight, values = values)
    }

    /** Accumulated retention time of the ComponentState. Only visited states will be included. */
    fun summed(): FrequencyTable<T> = xDuration().zip(this.values)
        .groupBy { (_, value) -> value }
        .map { it.key to it.value.sumOf { (it.first) } }.toMap()


    override fun  statisticsSummary() = statsData().statisticalSummary()

    fun statsData(): LevelStatsData<T> {
        require(values.isNotEmpty()) { "data must not be empty when preparing statistics of $name" }

        val valuesLst = values.toList()

        val timepointsExt = timestamps + env.now.value
        val durations = timepointsExt.toMutableList().zipWithNext { first, second -> second - first }

        return LevelStatsData(valuesLst, timestamps, durations)
    }


    /** Returns the step function of this monitored value along the time axis. */
    override fun stepFun() = statsData().stepFun()


    override val info: Jsonable
        get() = ImplementMe()
}