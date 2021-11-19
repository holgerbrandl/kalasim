package org.kalasim.misc

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.kalasim.asCMPairList
import org.kalasim.monitors.ValueTimeline

internal fun Collection<Double>.median() = Median().evaluate(toDoubleArray())


// copied from krangl
fun <T : Number> List<T>.cumSum(): Iterable<Double> {
    return drop(1).fold(listOf(first().toDouble()), { list, curVal -> list + (list.last().toDouble() + curVal.toDouble()) })
}



fun List<StatisticalSummary>.merge() = AggregateSummaryStatistics.aggregate(this)

fun <T> List<ValueTimeline<T>>.mergeStats() = map{it.statisticsSummary()}.merge()
//
//fun List<MetricTimeline>.mergeStats() = flatMap { it.statisticsSummary().pmf }.groupBy { it.first }
//        .map { it.key to it.value.sumOf { it.second } }.asCMPairList().run {
//            EnumeratedDistribution(this)
//        }

fun <E> List<EnumeratedDistribution<E>>.merge()= flatMap { it.pmf }.groupBy { it.first }
    .map { it.key to it.value.sumOf { it.second } }.asCMPairList().run {
        EnumeratedDistribution(this)
    }

