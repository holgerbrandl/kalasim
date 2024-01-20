package org.kalasim.test

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import org.junit.Test
import org.kalasim.Environment
import org.kalasim.normal
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

class DistributionTests {

    @Test
    fun `it should correctly rectify a normal distribution`() {

        Environment().apply {
            val tbf = normal(3.days, 2.days, rectify = true)
            val tbf2 = normal(3, 2, rectify = true)

            repeat(1000) {
                tbf().toDouble(DurationUnit.HOURS) shouldBeGreaterThanOrEqual 0.0
                tbf2.sample() shouldBeGreaterThanOrEqual 0.0
            }
        }
    }
}