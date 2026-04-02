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
import kotlin.time.toDuration

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
    ConstantRealDistribution(value.toDouble(DurationUnit.SECONDS)),
    DurationUnit.SECONDS
)

val Number.perSecond get() = Rate(this, DurationUnit.SECONDS)
val Number.perMinute get() = Rate(this, DurationUnit.MINUTES)
val Number.perHour get() = Rate(this, DurationUnit.HOURS)

data class Rate(
    val eventsPerTimeUnit: Number,
    val timeUnit: DurationUnit = DurationUnit.SECONDS
) {
    init {
        require(eventsPerTimeUnit.toDouble() >= 0) { "Rate must be non-negative" }
    }

    val mean: Duration
        get() = if (eventsPerTimeUnit == 0.0) {
            Duration.INFINITE
        } else {
            (1.0 / eventsPerTimeUnit.toDouble()).toDuration(timeUnit)
        }
}


/**
 * Exponential distribution with built-in support for controlled-randomization.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.exponential(mean: Number) = ExponentialDistribution(env.rg, mean.toDouble())
fun SimContext.exponential(rate: Rate) = DurationDistribution(
    ExponentialDistribution(env.rg, 1.0/rate.eventsPerTimeUnit.toDouble()),
    rate.timeUnit,
)

fun interface DistributionBuilder {
    fun build(environment: Environment): RealDistribution
}

/**
 * Creates an exponential distribution builder with the specified mean.
 *
 * The exponential distribution is commonly used to model the time between events in a Poisson point process,
 * i.e., a process in which events occur continuously and independently at a constant average rate.
 *
 * @param mean The mean (expected value) of the exponential distribution. Must be a positive number.
 * @return A [DistributionBuilder] that creates an exponential distribution when built with an environment.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun exponential(mean: Number) = DistributionBuilder { it.exponential(mean) }

/**
 * Creates an exponential distribution builder with the specified mean duration.
 *
 * The exponential distribution is commonly used to model the time between events in a Poisson point process,
 * i.e., a process in which events occur continuously and independently at a constant average rate.
 *
 * @param mean The mean (expected value) of the exponential distribution as a [Duration]. Must be positive.
 * @return A [DurationDistributionBuilder] that creates an exponential duration distribution when built with an environment.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun exponential(mean: Duration) = DurationDistributionBuilder { it.exponential(mean) }

/**
 * Creates a normal (Gaussian) distribution builder with the specified mean and standard deviation.
 *
 * The normal distribution is a continuous probability distribution that is symmetric around the mean,
 * showing that data near the mean are more frequent in occurrence than data far from the mean.
 *
 * @param mean The mean (expected value) of the normal distribution. Defaults to 0.
 * @param sd The standard deviation of the normal distribution. Must be positive. Defaults to 1.
 * @param rectify If true, negative samples are replaced with 0. Defaults to false.
 * @return A [DistributionBuilder] that creates a normal distribution when built with an environment.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun normal(mean: Number = 0, sd: Number = 1, rectify: Boolean = false) =
    DistributionBuilder { it.normal(mean, sd, rectify) }

/**
 * Creates a normal (Gaussian) distribution builder with the specified mean and standard deviation as durations.
 *
 * The normal distribution is a continuous probability distribution that is symmetric around the mean,
 * showing that data near the mean are more frequent in occurrence than data far from the mean.
 *
 * @param mean The mean (expected value) of the normal distribution as a [Duration].
 * @param sd The standard deviation of the normal distribution as a [Duration]. Must be positive.
 * @param rectify If true, negative samples are replaced with 0. Defaults to false.
 * @return A [DurationDistributionBuilder] that creates a normal duration distribution when built with an environment.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun normal(mean: Duration, sd: Duration, rectify: Boolean = false) =
    DurationDistributionBuilder { it.normal(mean, sd, rectify) }


fun interface DurationDistributionBuilder {
    fun build(environment: Environment): DurationDistribution
}


data class DurationDistributionBuilderImpl(val unit: DurationUnit, val distBuilder: DistributionBuilder) :
    DurationDistributionBuilder {
    override fun build(environment: Environment): DurationDistribution =
        DurationDistribution(distBuilder.build(environment), unit)
}


// todo reassess for api redundancy
val DistributionBuilder.seconds get() = DurationDistributionBuilderImpl(DurationUnit.SECONDS, this)
val DistributionBuilder.minutes get() = DurationDistributionBuilderImpl(DurationUnit.MINUTES, this)
val DistributionBuilder.hours get() = DurationDistributionBuilderImpl(DurationUnit.HOURS, this)
val DistributionBuilder.days get() = DurationDistributionBuilderImpl(DurationUnit.DAYS, this)


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
fun SimContext.normal(mean: Number = 0, sd: Number = 1, rectify: Boolean = false): NormalDistribution = if (rectify) {
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
fun SimContext.normal(mean: Duration, sd: Duration, rectify: Boolean = false): DurationDistribution = if (rectify) {
    RectifiedNormalDistribution(env.rg, mean.inSeconds, sd.inSeconds)
} else {
    NormalDistribution(env.rg, mean.inSeconds, sd.inSeconds)
}.seconds


private class RectifiedNormalDistribution(rng: RandomGenerator, mean: Double, sd: Double) :
    NormalDistribution(rng, mean, sd) {

    override fun sample(sampleSize: Int): DoubleArray = Array(sampleSize) { sample() }.toDoubleArray()

    override fun sample(): Double = super.sample().let { if (it < 0) 0.0 else it }
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


/**
 * Binomial distribution with built-in support for controlled-randomization.
 *
 * The binomial distribution models the number of successes in a fixed number of independent trials,
 * each with the same probability of success.
 *
 * @param trials The number of trials. Must be a non-negative integer.
 * @param probability The probability of success on each trial. Must be between 0 and 1 (inclusive).
 * @return A binomial distribution configured with the environment's random generator.
 *
 * For additional details see https://www.kalasim.org/basics/#randomness-distributions.
 */
fun SimContext.binomial(trials: Number, probability: Number) =
    BinomialDistribution(env.rg, trials.toInt(), probability.toDouble())

//
// date utils for distributions

data class DurationDistribution(val dist: RealDistribution, val unit: DurationUnit) {
    operator fun invoke() = sample()
    fun sample() = when (unit) {
        DurationUnit.SECONDS -> dist().seconds
        DurationUnit.MINUTES -> dist().minutes
        DurationUnit.HOURS -> dist().hours
        DurationUnit.DAYS -> dist().days
        else -> ImplementMe()
    }
}

val RealDistribution.seconds get() = DurationDistribution(this, DurationUnit.SECONDS)
val RealDistribution.minutes get() = DurationDistribution(this, DurationUnit.MINUTES)
val RealDistribution.hours get() = DurationDistribution(this, DurationUnit.HOURS)
val RealDistribution.days get() = DurationDistribution(this, DurationUnit.DAYS)

data class IntegerDurationDistribution(val unit: DurationUnit, val dist: IntegerDistribution) {
    fun invoke() = sample()
    fun sample() = when (unit) {
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
