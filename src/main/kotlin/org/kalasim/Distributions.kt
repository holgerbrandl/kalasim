package org.kalasim

import org.apache.commons.math3.distribution.*

/** Distribution support API */

fun Number.asDist() = ConstantRealDistribution(this.toDouble())

fun fixed(value: Number) = ConstantRealDistribution(value.toDouble())

operator fun RealDistribution.invoke(): Double = sample()
operator fun IntegerDistribution.invoke(): Int = sample()


fun Component.exponential(mean: Number) = env.exponential(mean)
fun Environment.exponential(mean: Number) = ExponentialDistribution(rg, mean.toDouble())


fun Component.normal(mean: Number = 0, sd: Number = 1) = env.normal(mean, sd)
fun Environment.normal(mean: Number = 0, sd: Number = 1) = NormalDistribution(rg, mean.toDouble(), sd.toDouble())


fun Component.discreteUniform(lower: Int, upper: Int) = env.discreteUniform(lower, upper)
fun Environment.discreteUniform(lower: Int, upper: Int) = UniformIntegerDistribution(rg, lower, upper)


fun Component.uniform(lower: Number = 0, upper: Number = 1) = env.uniform(lower, upper)
fun Environment.uniform(lower: Number = 1, upper: Number = 0) =
    UniformRealDistribution(rg, lower.toDouble(), upper.toDouble())



fun <T> Component.enumerated(vararg elements: T) = enumerated((elements.map { it to 1.0 / elements.size }).toMap())
fun <T> Component.enumerated(elements: Map<T, Double>) = env.enumerated(elements)

@JvmName("enumeratedArray")
fun <T> Environment.enumerated(elements: Array<T>) : EnumeratedDistribution<T> = enumerated(*elements)
fun <T> Environment.enumerated(vararg elements: T) = enumerated((elements.map { it to 1.0 / elements.size }).toMap())
fun <T> Environment.enumerated(elements: Map<T, Double>) = EnumeratedDistribution(rg, elements.toList().asCMPairList())



typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }
fun <T, S> Map<T, S>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.key, it.value) }

