package org.kalasim

import org.apache.commons.math3.distribution.*
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.misc.ImplementMe
import org.kalasim.misc.asCMPairList
import java.util.*
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


//@Deprecated("Use fixed instead", ReplaceWith("constant(this)"))
//fun Number.asDist() = ConstantRealDistribution(this.toDouble())

/**
 * Constant distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun constant(value: Number) = ConstantRealDistribution(value.toDouble())

/**
 * Constant distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun constant(value: Duration) = DurationDistribution(
    DurationUnit.SECONDS,
    ConstantRealDistribution(value.toDouble(DurationUnit.SECONDS))
)


/**
 * Exponential distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.exponential(mean: Number) = ExponentialDistribution(env.rg, mean.toDouble())

/**
 * Exponential distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.exponential(mean: Duration) = ExponentialDistribution(env.rg, mean.inSeconds).seconds

/**
 * Normal distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.normal(mean: Number = 0, sd: Number = 1, rectify: Boolean = false): NormalDistribution = if(rectify) {
    RectifiedNormalDistribution(env.rg, mean.toDouble(), sd.toDouble())
} else {
    NormalDistribution(env.rg, mean.toDouble(), sd.toDouble())
}


// for consistency also provide sampling via invoke for common other distribution
operator fun NormalDistribution.invoke(): Double = sample()
operator fun ExponentialDistribution.invoke(): Double = sample()


/**
 * Normal distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.normal(mean: Duration, sd: Duration, rectify: Boolean = false): DurationDistribution = if(rectify) {
    RectifiedNormalDistribution(env.rg, mean.inSeconds, sd.inSeconds)
} else {
    NormalDistribution(env.rg, mean.inSeconds, sd.inSeconds)
}.seconds


private class RectifiedNormalDistribution(rng: RandomGenerator, mean: Double, sd: Double) :
    NormalDistribution(rng, mean, sd) {

    override fun sample(sampleSize: Int): DoubleArray = Array(sampleSize) { sample() }.toDoubleArray()

    override fun sample(): Double = super.sample().let { if(it < 0) 0.0 else it }
}


/**
 * Triangular distribution, see https://en.wikipedia.org/wiki/Triangular_distribution, with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.triangular(lowerLimit: Number, mode: Number, upperLimit: Number): TriangularDistribution =
    TriangularDistribution(env.rg, lowerLimit.toDouble(), mode.toDouble(), upperLimit.toDouble())


/**
 * Triangular distribution, see https://en.wikipedia.org/wiki/Triangular_distribution, with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.triangular(lowerLimit: Duration, mode: Duration, upperLimit: Duration): DurationDistribution =
    TriangularDistribution(env.rg, lowerLimit.inSeconds, mode.inSeconds, upperLimit.inSeconds).seconds


/**
 * Uniform distribution with built-in support for controlled-randomization.
 *
 * @param lower Lower bound of this distribution (inclusive).
 * @param upper Upper bound of this distribution (exclusive).
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.uniform(lower: Number = 0, upper: Number = 1) =
    UniformRealDistribution(env.rg, lower.toDouble(), upper.toDouble())


/**
 * Uniform distribution with built-in support for controlled-randomization.
 *
 * Internally the duration range is resolved to seconds for the sampling process. The upper limit is exclusive.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.uniform(lower: Duration, upper: Duration) =
    UniformRealDistribution(env.rg, lower.inSeconds, upper.inSeconds).seconds


//
// date utils for distributions

data class DurationDistribution(val unit: DurationUnit, val dist: RealDistribution) {
    operator fun invoke() = sample()
    fun sample() = when(unit) {
        DurationUnit.SECONDS -> dist().seconds
        DurationUnit.MINUTES -> dist().minutes
        DurationUnit.HOURS -> dist().hours
        DurationUnit.DAYS -> dist().days
        else -> ImplementMe()
    }
}

val RealDistribution.seconds get() = DurationDistribution(DurationUnit.SECONDS, this)
val RealDistribution.minutes get() = DurationDistribution(DurationUnit.MINUTES, this)
val RealDistribution.hours get() = DurationDistribution(DurationUnit.HOURS, this)
val RealDistribution.days get() = DurationDistribution(DurationUnit.DAYS, this)

data class IntegerDurationDistribution(val unit: DurationUnit, val dist: IntegerDistribution) {
    fun invoke() = sample()
    fun sample() = when(unit) {
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


/**
 * Uniform discrete distribution with built-in support for controlled-randomization.
 *
 * @param range which is consumed including the end into a distribution.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.discreteUniform(range: IntRange) = discreteUniform(range.first, range.last)

/**
 * Uniform discrete distribution with built-in support for controlled-randomization.
 *
 * @param lower Lower bound of this distribution (inclusive).
 * @param upper Upper bound of this distribution (exclusive).
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.discreteUniform(lower: Int, upper: Int) = UniformIntegerDistribution(env.rg, lower, upper)


//
// Enumerations
//

operator fun <E> EnumeratedDistribution<E>.invoke(): E = sample()
operator fun <E> EnumeratedDistribution<E>.get(key: E): Double = pmf.first { it.key == key }.value


/**
 * Discrete uniform distribution over an array of `elements` with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#enumerations.
 */
@JvmName("enumeratedArray")
fun <T> SimContext.enumerated(elements: Array<T>): EnumeratedDistribution<T> = enumerated(*elements)

/**
 * Discrete uniform distribution over an array of `elements` with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#enumerations.
 */
fun <T> SimContext.enumerated(vararg elements: T) = enumerated((elements.map { it to 1.0 / elements.size }).toMap())

/**
 * Discrete  distribution over an array of `elements` with defined weights. Weights are internally normalized to 1
 * Has built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#enumerations.
 */
fun <T> SimContext.enumerated(elements: Map<T, Double>) =
    EnumeratedDistribution(env.rg, elements.toList().asCMPairList())


//
// ID Generation
//

/**
 * Sample UUIDs with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#enumerations
 */

fun SimContext.uuid(): UUID = UUID.nameUUIDFromBytes(env.rg.nextLong().toString().toByteArray())
