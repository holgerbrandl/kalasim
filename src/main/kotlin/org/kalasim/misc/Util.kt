package org.kalasim.misc

import com.google.gson.GsonBuilder
import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.roundToInt

internal fun Any.printThis() {
    println(toString())
}

//class Histogram()

// https://stackoverflow.com/questions/10786465/how-to-generate-bins-for-histogram-using-apache-math-3-0-in-java
internal fun DescriptiveStatistics.buildHistogram(
    binCount: Int = 30,
    lowerBound: Double? = null,
    upperBound: Double? = null,
): List<Pair<Pair<Double, Double>, Long>> {

    require(lowerBound == null && upperBound == null) { ImplementMe() }

    val histogram = LongArray(binCount)
    val distribution = EmpiricalDistribution(binCount)
    distribution.load(values)
    var k = 0
    for (ss in distribution.binStats) {
        histogram[k++] = ss.n
    }

    val intervals = (listOf(distribution.binStats[0].min) + distribution.upperBounds.toList()).zipWithNext()

    return intervals.zip(distribution.binStats.map { it.n })
}


internal fun ImplementMe() {
    TODO("Not yet implemented. Please file a ticket under https://github.com/holgerbrandl/kalasim/issues")
}


internal typealias  Histogram<T> = List<Pair<T, Long>>

internal fun DescriptiveStatistics.printHistogram() {
    buildHistogram()
        // pretty print ranges
        .printHistogram()

}

internal fun <T> Histogram<T>.printHistogram(
    colWidth: Double = 40.0,
    sortByWeight: Boolean = false,
    values: List<T>? = null
) {

    // if value range is provided adopt it
    var hist = if (values != null) {
        val parList = this.partition { (bin, _) -> values.contains(bin) }
        // also complement missing values

        val valueExt = parList.first.toMutableList().apply {
            values.minus(this.toMap().keys).forEach {
                add(it to 0.toLong())
            }
        }

        (valueExt as List<Pair<Any, Long>>) + ("rest" to parList.second.sumOf { it.second })
    } else {
        this
    }.run {
        if (sortByWeight) {
            sortedByDescending { it.second }
        } else this
    }

    val n = hist.sumOf { it.second }

    listOf("bin", "entries", "pct", "").zip(listOf(17, 7, 4, colWidth.toInt())).map { it.first.padStart(it.second) }
        .joinToString(" | ").printThis()

    hist.forEach { (bin, binValue) ->
        val scaledValue = binValue.toDouble() / n

        val range = if (bin is Pair<*, *>) {
            "[${JSON_DF.format(bin.first)}, ${JSON_DF.format(bin.second)}]"
        } else {
            bin.toString()
        }

        val pct = JSON_DF.format(scaledValue)
        val stars = "*".repeat((scaledValue * colWidth).roundToInt()).padEnd(colWidth.roundToInt(), ' ')
        listOf(range.padEnd(17), binValue.toString().padStart(7), pct.padStart(4), stars).joinToString(" | ")
            .printThis()
    }

    println()
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
var JSON_INDENT = 2

typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCM(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }


// from https://stackoverflow.com/questions/46895140/how-to-perform-action-for-all-combinations-of-elements-in-lists
// ternary and higher order: https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of // todo simply add up to 8ary?
/** Calculate all pairwise combination of 2 lists.*/
 fun <A, B> cartesianProduct(
    listA: Iterable<A>,
    listB: Iterable<B>
): Sequence<Pair<A, B>> =
    sequence {
        listA.forEach { a ->
            listB.forEach { b ->
                yield(a to b)
            }
        }
    }

