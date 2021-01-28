package org.kalasim.monitors

import org.kalasim.misc.ImplementMe
import org.kalasim.misc.Jsonable
import org.kalasim.misc.printConsole
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

/**
 * Frequency tally levels irrespective of current (simulation) time.
 *
 * @sample org.kalasim.misc.DokkaExamplesKt.freqLevelDemo
 */
open class FrequencyStatisticMonitor<T>(
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
        frequencies.mapValues { it.value.toDouble() }
            .printConsole(values = values, sortByWeight = sortByWeight)
//        frequencies.keys.asSequence().map {
//            println("${it}\t${getPct(it)}\t${frequencies[it]}")
//        }
    }

    open fun getPct(value: T): Double = frequencies[value]!!.toDouble() / total

    override val info: Jsonable
        get() = ImplementMe()
}