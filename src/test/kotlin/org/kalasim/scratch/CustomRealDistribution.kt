package org.kalasim.scratch

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.*

fun main() {
    val sim = createSimulation {
        enableComponentLogger()

        val iat = object : SimpleRealDistribution(rg) {
            override fun density(x: Double): Double {
                val isDay = (now.toTickTime().value % 24) in 6.0..18.0

                return exponential(if(isDay) 0.25 else 0.05).sample()
            }
        }
        // apply dimension `hours` to it
        ComponentGenerator(iat = iat.hours) { "foo" }
    }

    sim.run(1.week)
}

internal abstract class SimpleRealDistribution(rg: RandomGenerator) : AbstractRealDistribution(rg) {
    override fun cumulativeProbability(x: Double): Double {
        TODO("Not yet implemented")
    }

    override fun getNumericalMean(): Double {
        TODO("Not yet implemented")
    }

    override fun getNumericalVariance(): Double {
        TODO("Not yet implemented")
    }

    override fun getSupportLowerBound(): Double {
        return 0.0
    }

    override fun getSupportUpperBound(): Double {
        return Double.MAX_VALUE
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun isSupportLowerBoundInclusive(): Boolean {
        TODO("Not yet implemented")
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun isSupportUpperBoundInclusive(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSupportConnected(): Boolean {
        TODO("Not yet implemented")
    }
}