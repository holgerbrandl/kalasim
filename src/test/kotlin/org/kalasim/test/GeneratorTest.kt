package org.kalasim.test

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.junit.Test
import org.kalasim.*
import kotlin.test.assertEquals

class GeneratorTest {

    class Customer : Component() {}

    @Test
    fun testCustomerGenerator() {

        val tc = TraceCollector()

        Environment().apply {

            addEventListener(tc)

            ComponentGenerator(iat = ExponentialDistribution(2.0), total = 4) { Customer() }
        }.run(100.0)

        val customers = tc.traces
            .filterIsInstance<InteractionEvent>()
            .map { it.source }
            .filterNotNull().distinct()
            .filter { it.name.startsWith("Customer") }

        assertEquals(4, customers.size, "incorrect expected customer cont")
    }
}