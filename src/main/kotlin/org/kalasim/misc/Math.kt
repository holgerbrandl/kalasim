package org.kalasim.misc

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.stat.descriptive.*
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.kalasim.monitors.ValueTimeline

fun Collection<Double>.median() = Median().evaluate(toDoubleArray())

val DescriptiveStatistics.median: Double
    get() = Median().evaluate(values)


// copied from krangl
fun <T : Number> List<T>.cumSum(): Iterable<Double> =
    drop(1).fold(listOf(first().toDouble())) { list, curVal ->
        list + (list.last().toDouble() + curVal.toDouble())
    }

fun List<StatisticalSummary>.merge(): StatisticalSummaryValues = AggregateSummaryStatistics.aggregate(this)

fun <T> List<ValueTimeline<T>>.mergeStats() = map { it.statisticsSummary() }.merge()
//
//fun List<MetricTimeline>.mergeStats() = flatMap { it.statisticsSummary().pmf }.groupBy { it.first }
//        .map { it.key to it.value.sumOf { it.second } }.asCMPairList().run {
//            EnumeratedDistribution(this)
//        }

fun <E> List<EnumeratedDistribution<E>>.merge() =
    flatMap { it.pmf }
        .groupBy { it.first }
        .map { kv -> kv.key to kv.value.sumOf { it.second } }
        .asCMPairList()
        .run {
            EnumeratedDistribution(this)
        }

