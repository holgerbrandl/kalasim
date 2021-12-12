package org.kalasim.test

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.junit.Test
import org.kalasim.*
import kotlin.test.assertEquals
import kotlin.test.fail

class GeneratorTest {

    class Customer : Component()

    @Test
    fun testCustomerGenerator() {

        val eventLog = EventLog()

        Environment().apply {
            addEventListener(eventLog)

            ComponentGenerator(iat = ExponentialDistribution(2.0), total = 4) { Customer() }
        }.run(100.0)

        val customers = eventLog()
            .filterIsInstance<EntityCreatedEvent>()
            .map { it.simEntity }.distinct()
            .filter { it.name.startsWith("Customer") }

        assertEquals(4, customers.size, "incorrect expected customer cont")
    }


    @Test
    fun `it should allow to stop a generator from outside`() = createTestSimulation {
        val cg = ComponentGenerator(iat = constant(1), keepHistory = true) { it.toString() }

        run(10)

        cg.cancel()

        cg.addConsumer { fail() }

        run(10)
    }

    @Test
    fun `it should allow to stop a generator from inside`() = createTestSimulation {
        val cg = ComponentGenerator(iat = constant(1), keepHistory = true) { it.toString() }

        run(10)

        cg.addConsumer { cg.cancel() }

        run(3)

        cg.addConsumer { fail() }

        run(10)
    }
}