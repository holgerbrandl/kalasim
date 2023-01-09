package org.kalasim

import org.apache.commons.math3.distribution.*
import org.kalasim.misc.ImplementMe
import java.lang.Double.min
import java.util.*
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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

fun constant(value: Duration) = RealDurationDistribution(
    DurationUnit.SECONDS,
    ConstantRealDistribution(value.toDouble(DurationUnit.SECONDS))
)


/**
 * See https://www.kalasim.org/basics/#randomness-distributions
 *
 * A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/ExponentialDistribution.html */
fun SimContext.exponential(mean: Number) = ExponentialDistribution(env.rg, mean.toDouble())

/**
 * See https://www.kalasim.org/basics/#randomness-distributions
 *
 * A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/ExponentialDistribution.html */
fun SimContext.exponential(mean: Duration) = ExponentialDistribution(env.rg, mean.inWholeSeconds.toDouble()).seconds

/**
 * See https://www.kalasim.org/basics/#randomness-distributions
 *
 * A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/NormalDistribution.html */
fun SimContext.normal(mean: Number = 0, sd: Number = 1) = NormalDistribution(env.rg, mean.toDouble(), sd.toDouble())


/** Triangular distribution, see https://en.wikipedia.org/wiki/Triangular_distribution. A controlled randomization wrapper around https://commons.apache.org/proper/commons-math/javadocs/api-3.6/org/apache/commons/math3/distribution/TriangularDistribution.html */
fun SimContext.triangular(lowerLimit: Number, mode: Number, upperLimit: Number) =
    TriangularDistribution(env.rg, lowerLimit.toDouble(), mode.toDouble(), upperLimit.toDouble())



/** Clip the values of the distribution to the provided interval. */
// we could also adopt kotlin stdlib conventions and use coerceIn, coerceAtLeast and coerceAtMost
fun RealDistribution.clip(lower: Number = 0, upper: Number = Double.MAX_VALUE) =
    Clipper(this, lower.toDouble(), upper.toDouble())


class Clipper internal constructor(val dist: RealDistribution, val lower: Double, val upper: Double) {
    operator fun invoke(): Double = min(max(dist(), lower), upper)
}


fun SimContext.uniform(lower: Number = 0, upper: Number = 1) =
    UniformRealDistribution(env.rg, lower.toDouble(), upper.toDouble())



fun SimContext.uniform(lower: Duration, upper: Duration) =
    UniformRealDistribution(env.rg, lower.inWholeSeconds.toDouble(), upper.inWholeSeconds.toDouble()).seconds


//
// date utils for distributions

data class RealDurationDistribution(val unit: DurationUnit, val dist: RealDistribution){
    operator fun invoke() = sample()
    fun sample() = when(unit){
        DurationUnit.SECONDS -> dist().seconds
        DurationUnit.MINUTES -> dist().minutes
        DurationUnit.HOURS -> dist().hours
        DurationUnit.DAYS -> dist().days
        else -> ImplementMe()
    }
}

val RealDistribution.seconds get() = RealDurationDistribution(DurationUnit.SECONDS, this)
val RealDistribution.minutes get() = RealDurationDistribution(DurationUnit.MINUTES, this)
val RealDistribution.hours get() = RealDurationDistribution(DurationUnit.HOURS, this)
val RealDistribution.days get() = RealDurationDistribution(DurationUnit.DAYS, this)


data class IntegerDurationDistribution(val unit: DurationUnit, val dist: IntegerDistribution){
    fun invoke() = sample()
    fun sample() = when(unit){
        DurationUnit.SECONDS -> dist().seconds
        DurationUnit.MINUTES -> dist().minutes
        DurationUnit.DAYS -> dist().days
        else -> ImplementMe()
    }
}

val IntegerDistribution.seconds get() = IntegerDurationDistribution(DurationUnit.SECONDS, this)
val IntegerDistribution.minutes get() = IntegerDurationDistribution(DurationUnit.MINUTES, this)
val IntegerDistribution.days get() = IntegerDurationDistribution(DurationUnit.DAYS, this)


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
operator fun <E> EnumeratedDistribution<E>.get(key: E): Double = pmf.first { it.key == key }.value


// since it's common that users want to create an integer distribution from a range, we highlight the incorrect API usage
//@Deprecated(
//    "To sample from an integer range, use discreteUniform instead for better efficiency",
//    replaceWith = ReplaceWith("discreteUniform(range.first, range.laste)")
//)
//fun Environment.enumerated(range: IntRange): EnumeratedDistribution<Int> = enumerated(*(range.toList().toTypedArray()))

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
