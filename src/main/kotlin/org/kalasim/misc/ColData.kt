package org.kalasim.misc

import kotlin.math.roundToInt

// alernative to frequency (because not necessarily a distribution
typealias ColData<T> = Map<T, Double>


// displayers


internal fun <T> ColData<T>.printConsole(
    colWidth: Double = 40.0,
    sortByWeight: Boolean = false,
    values: List<T>? = null
) {
    // if value range is provided adopt it
    val hist = if (values != null) {
        val parList = this.toList().partition { (bin, _) -> values.contains(bin) }
        // also complement missing values

        val valueExt = parList.first.toMutableList().apply {
            values.minus(this.toMap().keys).forEach {
                add(it to 0.toDouble())
            }
        }

        (valueExt as List<Pair<Any, Double>>) + ("rest" to parList.second.sumOf { it.second })
    } else {
        this.toList()
    }.run {
        if (sortByWeight) {
            sortedByDescending { it.second }
        } else this
    }

    val n = hist.sumOf { it.second }

    listOf("bin", "values", "pct", "").zip(listOf(17, 7, 4, colWidth.toInt())).map { it.first.padStart(it.second) }
        .joinToString(" | ").printThis()

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


