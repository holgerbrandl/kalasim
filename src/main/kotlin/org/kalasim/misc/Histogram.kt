package org.kalasim.misc

import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import kotlin.math.roundToInt



data class HistogramBin(val lowerBound: Double, val upperBound: Double, val value: Long)

class Histogram(val bins: List<HistogramBin>){
    val n = bins.sumOf { it.value }
    val legacyFormat = bins.map{ (it.lowerBound to it.upperBound) to it.value}
}

// extensions to build histogram


// https://stackoverflow.com/questions/10786465/how-to-generate-bins-for-histogram-using-apache-math-3-0-in-java
internal fun DescriptiveStatistics.buildHistogram(
    binCount: Int = 30,
    lowerBound: Double? = null,
    upperBound: Double? = null,
): Histogram {

    require(lowerBound == null && upperBound == null) { ImplementMe() }

    val histogram = LongArray(binCount)
    val distribution = EmpiricalDistribution(binCount)
    distribution.load(values)
    var k = 0
    for (ss in distribution.binStats) {
        histogram[k++] = ss.n
    }

    val intervals = (listOf(distribution.binStats[0].min) + distribution.upperBounds.toList()).zipWithNext()

    return intervals.zip(distribution.binStats.map { it.n }).map{
        HistogramBin(it.first.first, it.first.second, it.second)
    }.let { Histogram(it) }
}


// Console backend

internal fun Histogram.printHistogram(colWidth: Double = 40.0) {
    listOf("bin", "entries", "pct", "").zip(listOf(17, 7, 4, colWidth.toInt())).map { it.first.padStart(it.second) }
        .joinToString(" | ").printThis()

    bins.forEach { (lower, upper, value) ->
        val scaledValue : Double = value.toDouble() / n

        val range = "[${JSON_DF.format(lower)}, ${JSON_DF.format(upper)}]"

        val pct = JSON_DF.format(scaledValue)
        val stars = "*".repeat((scaledValue * colWidth).roundToInt()).padEnd(colWidth.roundToInt(), ' ')
        listOf(range.padEnd(17), value.toString().padStart(7), pct.padStart(4), stars).joinToString(" | ")
            .printThis()
    }

    println()
}