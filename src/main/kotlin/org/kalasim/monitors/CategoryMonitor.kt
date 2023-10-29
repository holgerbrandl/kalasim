package org.kalasim.monitors

import org.kalasim.analysis.snapshot.FrequencyTableSnapshot
import org.kalasim.misc.*
import org.koin.core.Koin
import kotlin.math.roundToInt

/**
 * Frequency tally levels irrespective of current (simulation) time.
 *
 * @sample org.kalasim.dokka.freqLevelDemo
 */
open class CategoryMonitor<T>(
    name: String? = null,
    koin: Koin = DependencyContext.get()
) : Monitor<T>(name, koin), ValueMonitor<T> {

//    override var enabled: Boolean = true
//        set(isEnabled) {
//            field = isEnabled
//
//            // todo is this what we want to do here?
////            reset()
//        }


    internal val frequencies = mutableMapOf<T, Long>()
        get() = ifEnabled { field }

    override fun addValue(value: T) {
        if(!enabled) return

        frequencies.merge(value, 1, Long::plus)
    }


    override fun reset() = frequencies.clear()

    open fun getPct(value: T): Double = frequencies[value]!!.toDouble() / total

    val total: Long
        get() = frequencies.values.sum()


    val statistics: FrequencyTable<T>
        get() = frequencies.mapValues { it.value.toDouble() }

    override val snapshot
        get() = FrequencyTableSnapshot(statistics)
}

fun <T> CategoryMonitor<T>.printHistogram(values: List<T>? = null, sortByWeight: Boolean = false) {
    println("Summary of: '$name'")
    println("# Records: $total")
    println("# Levels: ${frequencies.keys.size}")
    println()

    // todo make as pretty as in https://www.salabim.org/manual/Monitor.html
    println("Histogram of: '${name}'")
//        frequencies.mapValues { it.value.toDouble() }
    snapshot.counts.printConsole(values = values, sortByWeight = sortByWeight)
}


typealias FrequencyTable<T> = Map<T, Double>


fun <T> FrequencyTable<T>.printConsole(
    colWidth: Double = 40.0,
    sortByWeight: Boolean = false,
    values: List<T>? = null
) {
    // if value range is provided adopt it
    val hist = if(values != null) {
        val parList = this.toList().partition { (bin, _) -> values.contains(bin) }
        // also complement missing values

        val valueExt = parList.first.toMutableList().apply {
            values.minus(this.toMap().keys).forEach {
                add(it to 0.0)
            }
        }

        (valueExt) + ("rest" to parList.second.sumOf { it.second })
    } else {
        this.toList()
    }.run {
        if(sortByWeight) {
            sortedByDescending { it.second }
        } else this
    }

    val n = hist.sumOf { it.second }

    listOf("bin", "values", "pct", "")
        .zip(listOf(17, 7, 4, colWidth.toInt()))
        .joinToString(" | ") { it.first.padStart(it.second) }
        .printThis()

    hist.forEach { (bin, binValue) ->
        val scaledValue = binValue / n

        val pct = JSON_DF.format(scaledValue)
        val stars = "*".repeat((scaledValue * colWidth).roundToInt()).padEnd(colWidth.roundToInt(), ' ')
        listOf(
            bin.toString().padEnd(17),
            JSON_DF.format(binValue).padStart(7),
            pct.padStart(4),
            stars
        ).joinToString(" | ")
            .printThis()
    }

    println()
}

//https://stackoverflow.com/questions/64325428/kotlin-reduce-list-of-map-to-a-single-map
fun <T> List<CategoryMonitor<T>>.mergeStats(): FrequencyTable<T> = map { it.statistics }.run {
//    val maps = listOf(mapOf("fo" to 1, "fop" to 2), mapOf("bar" to 1, "fo" to 2))

    return flatMap { it.entries }
        .groupBy { it.key }
        .mapValues { it.value.sumOf { v -> v.value } }
}
