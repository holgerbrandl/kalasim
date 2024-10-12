package org.kalasim.test

import io.kotest.matchers.doubles.*
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.junit.jupiter.api.Test
import org.kalasim.*
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.misc.*
import org.kalasim.monitors.NumericStatisticMonitor
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class GeneratorTest {

    class Customer : Component()

    @Test
    fun testCustomerGenerator() {

        val eventLog = EventLog()

        Environment().apply {
            addEventListener(eventLog)

            ComponentGenerator(iat = ExponentialDistribution(2.0).minutes, total = 4) { Customer() }
        }.run(100.minutes)

        val customers = eventLog()
            .filterIsInstance<EntityCreatedEvent>()
            .map { it.entity }.distinct()
            .filter { it.name.startsWith("Customer") }

        assertEquals(4, customers.size, "incorrect expected customer cont")
    }


    @Test
    fun `it should allow to stop a generator from outside`() = createTestSimulation {
        val cg = ComponentGenerator(iat = constant(1).hours, keepHistory = true) { it.toString() }

        run(10.minutes)

        cg.cancel()

        cg.addConsumer { fail() }

        run(10.minutes)
    }

    @Test
    fun `it should allow sampling iat from a triangular distribution`() =
        createTestSimulation(enableComponentLogger = false) {
            val nsm = NumericStatisticMonitor()

            var lastCreation: SimTime = now

            ComponentGenerator(iat = triangular(4, 8, 10).days) {
                val timeSinceLastArrival = now - lastCreation
                nsm.addValue(timeSinceLastArrival.inDays)
                it.toString()
                lastCreation = now
            }

            run(10000.days)

            nsm.statistics().min shouldBeGreaterThanOrEqual 4.0
            nsm.statistics().max shouldBeLessThanOrEqual 10.0
            nsm.values.toList().map { it.roundAny(2) }
                .groupBy { it }
                .maxByOrNull { it.value.size }
                ?.key shouldBe 8.0.plusOrMinus(0.1)
        }

    @Test
    fun `it should allow to stop a generator from inside`() = createTestSimulation {
        val cg = ComponentGenerator(iat = constant(1).days, keepHistory = true) { it.toString() }

        run(10.days)

        cg.addConsumer { cg.cancel() }

        run(3.days)

        cg.addConsumer { fail() }

        run(10.days)
    }
}