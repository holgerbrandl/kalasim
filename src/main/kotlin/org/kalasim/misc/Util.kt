package org.kalasim.misc

import com.google.gson.GsonBuilder
import com.systema.analytics.es.misc.json
import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.roundToInt

internal fun Any.println() {
    println(toString())
}

// https://stackoverflow.com/questions/10786465/how-to-generate-bins-for-histogram-using-apache-math-3-0-in-java
internal fun buildHistogram(stats: DescriptiveStatistics, binCount: Int = 30): List<Pair<Pair<Double, Double>, Long>> {

    val histogram = LongArray(binCount)
    val distribution = EmpiricalDistribution(binCount)
    distribution.load(stats.values)
    var k = 0
    for (ss in distribution.binStats) {
        histogram[k++] = ss.n
    }

    val intervals = (listOf(distribution.binStats[0].min) + distribution.upperBounds.toList()).zipWithNext()

    return intervals.zip(distribution.binStats.map { it.n })
}

internal fun DescriptiveStatistics.printHistogram(name: String) {
    json {
        "name" to name
        "type" to this@printHistogram.javaClass.simpleName //"queue statistics"
        "entries" to n
        "mean" to mean
        "minimum" to min
        "maximum" to max
    }.toString().println()

    // also print histogram
    val histogram = buildHistogram(this, 10)

    // rescale max to column width
    val colWidth = 40.0
//    val total = histogram.sumOf { it.second.toDouble() }

    val histogramScaled = histogram.map { (range, value) -> range to colWidth * value / n }

    histogramScaled.forEach { (binInterval, binHeight) ->
        val range = "[${JSON_DF.format(binInterval.first)}, ${JSON_DF.format(binInterval.second)}]"
        val pct = JSON_DF.format(binHeight)
        val stars = "*".repeat(binHeight.roundToInt()).padEnd(colWidth.roundToInt(), ' ')
        (range.padEnd(15, ' ')  +pct.padStart(5) +" |" + stars + "|").println()
    }
    kotlin.io.println()
}

var JSON_DF = DecimalFormat("###.00")

// move away from main namespace
var TRACE_DF = DecimalFormat("###.00")



// MATH UTILS

internal fun Collection<Double>.median() = Median().evaluate(toDoubleArray())

//@Serializable
abstract class Jsonable {

    open fun toJson(): JSONObject = JSONObject(GSON.toJson(this))

    override fun toString(): String {
        return toJson().toString(JSON_INDENT)

        // todo get rid of gson here to simplify dependency tree
//        return GSON.toJson(this)
//        return Json.encodeToString(this)
    }
}

// https://futurestud.io/tutorials/gson-builder-special-values-of-floats-doubles
// https://github.com/google/gson/blob/master/UserGuide.md#null-object-support
internal val GSON by lazy {
    GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().serializeNulls().create()
}
var JSON_INDENT= 2