package org.kalasim

import org.apache.commons.math3.distribution.*
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.misc.ImplementMe
import org.kalasim.misc.asCMPairList
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
fun SimContext.exponential(mean: Duration) = ExponentialDistribution(env.rg, mean.doubleSeconds).seconds

/**
 * Normal distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.normal(mean: Number = 0, sd: Number = 1, rectify: Boolean = false): NormalDistribution = if(rectify){
    NormalDistribution(env.rg, mean.toDouble(), sd.toDouble())
}else{
    RectifiedNormalDistribution(env.rg, mean.toDouble(), sd.toDouble())
}

/**
 * Normal distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.normal(mean: Duration, sd: Duration, rectify: Boolean = false): DurationDistribution = if(rectify){
    NormalDistribution(env.rg, mean.doubleSeconds, sd.doubleSeconds)
}else{
    RectifiedNormalDistribution(env.rg, mean.doubleSeconds, sd.doubleSeconds)
}.seconds


private class RectifiedNormalDistribution(rng: RandomGenerator, mean:Double, sd:Double): NormalDistribution(rng, mean, sd){
    override fun probability(x0: Double, x1: Double): Double {
        return super.probability(x0, x1).let { if(it<0) 0.0 else it }
    }
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
    TriangularDistribution(env.rg, lowerLimit.doubleSeconds, mode.doubleSeconds, upperLimit.doubleSeconds).seconds

internal val Duration.doubleSeconds
    get() = inWholeSeconds.toDouble()


/** Clip the values of the distribution to the provided interval. */
// we could also adopt kotlin stdlib conventions and use coerceIn, coerceAtLeast and coerceAtMost
@Deprecated("Use rectify argument when creating normal distribution with normal(mean,sd, rectify=true)")
fun RealDistribution.clip(lower: Number = 0, upper: Number = Double.MAX_VALUE) =
    Clipper(this, lower.toDouble(), upper.toDouble())


@Deprecated("substituted with rectify argument in normal()")
class Clipper internal constructor(val dist: RealDistribution, val lower: Double, val upper: Double) {
    operator fun invoke(): Double = min(max(dist(), lower), upper)
}


/**
 * Uniform distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.uniform(lower: Number = 0, upper: Number = 1) =
    UniformRealDistribution(env.rg, lower.toDouble(), upper.toDouble())



/**
 * Uniform distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.uniform(lower: Duration, upper: Duration) =
    UniformRealDistribution(env.rg, lower.doubleSeconds, upper.doubleSeconds).seconds


//
// date utils for distributions

data class DurationDistribution(val unit: DurationUnit, val dist: RealDistribution){
    operator fun invoke() = sample()
    fun sample() = when(unit){
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


@JvmName("enumeratedArray")
/**
 * Discrete uniform distribution over an array of `elements` with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#enumerations.
 */
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
 */fun <T> SimContext.enumerated(elements: Map<T, Double>) =
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
