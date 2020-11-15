package org.github.holgerbrandl.kalasim.misc

import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import java.text.DecimalFormat

internal fun Any.println() {
    println(toString())
}

// https://stackoverflow.com/questions/10786465/how-to-generate-bins-for-histogram-using-apache-math-3-0-in-java
internal fun DescriptiveStatistics.buildHistogram(binCount: Int = 30): LongArray {
    val data = values

    val histogram = LongArray(binCount)
    val distribution = EmpiricalDistribution(binCount)
    distribution.load(data)
    var k = 0
    for (stats in distribution.binStats) {
        histogram[k++] = stats.n
    }

    return histogram
}

internal fun DescriptiveStatistics.printHistogram(name: String) {
    //TODO use json here
    println(
        """
       |name]\t\t${name}
       |entries\t\t${n}
       |mean\t\t${mean}
       |minimum\t\t${min}
       |maximum\t\t${max}
       |""".trimMargin()
    )

    // also print histogram
    val hist = buildHistogram().map { 1000 * it.toDouble() / sum }
    hist.forEachIndexed { idx, value ->
        idx.toString() + "\t" + "*".repeat(value.toInt()).padEnd(120, ' ').println()
    }
}

var JSON_DF = DecimalFormat("###.00")

// move away from main namespace
var TRACE_DF = DecimalFormat("###.00")