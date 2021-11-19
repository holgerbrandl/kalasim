package org.kalasim

import org.apache.commons.math3.distribution.*
import java.lang.Double.min
import kotlin.math.max

/** Distribution support API */

fun Number.asDist() = ConstantRealDistribution(this.toDouble())

fun fixed(value: Number) = ConstantRealDistribution(value.toDouble())

operator fun RealDistribution.invoke(): Double = sample()
operator fun IntegerDistribution.invoke(): Int = sample()
operator fun <E> EnumeratedDistribution<E>.invoke(): E = sample()


fun SimulationEntity.exponential(mean: Number) = env.exponential(mean)
fun Environment.exponential(mean: Number) = ExponentialDistribution(rg, mean.toDouble())


fun SimulationEntity.normal(mean: Number = 0, sd: Number = 1) = env.normal(mean, sd)
fun Environment.normal(mean: Number = 0, sd: Number = 1) = NormalDistribution(rg, mean.toDouble(), sd.toDouble())

class Clipper(val dist: RealDistribution, val lower: Double, val upper: Double) {
    fun invoke(): Double = min(max(dist(), lower), upper)
}

/** Clip the values of the distribution to the provided interval. */
// we could also adopt kotlin stdlib conventions and use coerceIn, coerceAtLeast and coerceAtMost
fun RealDistribution.clip(lower: Number = 0, upper: Number = Double.MAX_VALUE) =
    Clipper(this, lower.toDouble(), upper.toDouble())


fun SimulationEntity.discreteUniform(lower: Int, upper: Int) = env.discreteUniform(lower, upper)
fun Environment.discreteUniform(lower: Int, upper: Int) = UniformIntegerDistribution(rg, lower, upper)


fun SimulationEntity.uniform(lower: Number = 0, upper: Number = 1) = env.uniform(lower, upper)
fun Environment.uniform(lower: Number = 1, upper: Number = 0) =
    UniformRealDistribution(rg, lower.toDouble(), upper.toDouble())


fun <T> SimulationEntity.enumerated(vararg elements: T) =
    enumerated((elements.map { it to 1.0 / elements.size }).toMap())

fun <T> SimulationEntity.enumerated(elements: Map<T, Double>) = env.enumerated(elements)

@JvmName("enumeratedArray")
fun <T> Environment.enumerated(elements: Array<T>): EnumeratedDistribution<T> = enumerated(*elements)
fun <T> Environment.enumerated(vararg elements: T) = enumerated((elements.map { it to 1.0 / elements.size }).toMap())
fun <T> Environment.enumerated(elements: Map<T, Double>) = EnumeratedDistribution(rg, elements.toList().asCMPairList())



typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }
fun <T, S> Map<T, S>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.key, it.value) }

