package org.kalasim

import org.apache.commons.math3.distribution.*
import java.lang.Double.min
import java.util.*
import kotlin.math.max

/**
 * Distribution support API with controlled randomization via `env.rg`
 */




//
// Continuous Distributions
//

operator fun RealDistribution.invoke(): Double = sample()


@Deprecated("Use fixed instead", ReplaceWith("constant(this)"))
fun Number.asDist() = ConstantRealDistribution(this.toDouble())

fun constant(value: Number) = ConstantRealDistribution(value.toDouble())


/** A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/ExponentialDistribution.html */
fun SimContext.exponential(mean: Number) = ExponentialDistribution(env.rg, mean.toDouble())

/** A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/NormalDistribution.html */
fun SimContext.normal(mean: Number = 0, sd: Number = 1) = NormalDistribution(env.rg, mean.toDouble(), sd.toDouble())


/** Triangular distribution, see https://en.wikipedia.org/wiki/Triangular_distribution. A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/TriangularDistribution.html */
fun SimContext.triangular(lowerLimit: Number, mode: Number, upperLimit: Number) =
    TriangularDistribution(env.rg, lowerLimit.toDouble(), mode.toDouble(), upperLimit.toDouble())



/** Clip the values of the distribution to the provided interval. */
// we could also adopt kotlin stdlib conventions and use coerceIn, coerceAtLeast and coerceAtMost
fun RealDistribution.clip(lower: Number = 0, upper: Number = Double.MAX_VALUE) =
    Clipper(this, lower.toDouble(), upper.toDouble())


class Clipper internal constructor(val dist: RealDistribution, val lower: Double, val upper: Double) {
    fun invoke(): Double = min(max(dist(), lower), upper)
}


fun SimContext.uniform(lower: Number = 1, upper: Number = 0) =
    UniformRealDistribution(env.rg, lower.toDouble(), upper.toDouble())


//
// Discrete Distributions
//

operator fun IntegerDistribution.invoke(): Int = sample()


fun SimContext.discreteUniform(range: IntRange) = discreteUniform(range.first, range.last)
fun SimContext.discreteUniform(lower: Int, upper: Int) = UniformIntegerDistribution(env.rg, lower, upper)


//
// Enumerations
//

operator fun <E> EnumeratedDistribution<E>.invoke(): E = sample()


// since it's common that users want to create an integer distribution from a range, we highlight the incorrect API usage
@Deprecated(
    "To sample from an integer range, use discreteUniform instead for better efficiency",
    replaceWith = ReplaceWith("discreteUniform(range.first, range.laste)")
)
fun Environment.enumerated(range: IntRange) = enumerated(*(range.toList().toTypedArray()))

@JvmName("enumeratedArray")
fun <T> SimContext.enumerated(elements: Array<T>): EnumeratedDistribution<T> = enumerated(*elements)
fun <T> SimContext.enumerated(vararg elements: T) = enumerated((elements.map { it to 1.0 / elements.size }).toMap())
fun <T> SimContext.enumerated(elements: Map<T, Double>) =
    EnumeratedDistribution(env.rg, elements.toList().asCMPairList())

//
// Util
//

internal typealias   CMPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

internal fun <T, S> List<Pair<T, S>>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.first, it.second) }
internal fun <T, S> Map<T, S>.asCMPairList(): List<CMPair<T, S>> = map { CMPair(it.key, it.value) }


//
// ID Generation
//

fun SimContext.uuid(): UUID = UUID.nameUUIDFromBytes(env.rg.nextLong().toString().toByteArray())
