package org.kalasim.misc

import org.apache.commons.math3.stat.descriptive.rank.Median

internal fun Collection<Double>.median() = Median().evaluate(toDoubleArray())


// copied from krangl
fun <T : Number> List<T>.cumSum(): Iterable<Double> {
    return drop(1).fold(listOf(first().toDouble()), { list, curVal -> list + (list.last().toDouble() + curVal.toDouble()) })
}
