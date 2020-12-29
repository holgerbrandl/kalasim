package org.kalasim

import org.apache.commons.math3.distribution.*
import org.apache.commons.math3.random.RandomGenerator


@Suppress("unused")
fun Number.asConstantDist() = ConstantRealDistribution(this.toDouble())

fun Number.asDist() = ConstantRealDistribution(this.toDouble())

fun fixed(value: Double) = ConstantRealDistribution(value)


operator fun RealDistribution.invoke(): Double = sample()

fun uniform(lower: Number, upper: Number, rng: RandomGenerator? = null) = if (rng != null) {
    UniformRealDistribution(rng, lower.toDouble(), upper.toDouble())
} else {
    UniformRealDistribution(lower.toDouble(), upper.toDouble())
}

fun exponential(mean: Number, rng: RandomGenerator? = null) = if (rng != null) {
    ExponentialDistribution(rng, mean.toDouble())
} else {
    ExponentialDistribution(mean.toDouble())
}


fun normal(mean: Number, sd: Number, rng: RandomGenerator? = null) = if (rng != null) {
    NormalDistribution(rng, mean.toDouble(), sd.toDouble())
} else {
    ExponentialDistribution(mean.toDouble(), sd.toDouble())
}

