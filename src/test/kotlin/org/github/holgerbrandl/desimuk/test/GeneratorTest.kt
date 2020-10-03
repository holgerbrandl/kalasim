package org.github.holgerbrandl.desimuk.test

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.github.holgerbrandl.desimuk.*
import org.junit.Test
import kotlin.test.assertEquals

class TraceCollector : TraceListener {
    val traces = mutableListOf<TraceElement>()

    override fun processTrace(traceElement: TraceElement) {
        traces.add(traceElement)
    }
}

class GeneratorTest {

    class Customer : Component(){}

    @Test
    fun testCustomerGenerator() {

        val tc = TraceCollector()

        Environment().apply {

            addTraceListener(tc)

            ComponentGenerator(iat = ExponentialDistribution(2.0), total = 4) { Customer() }
        }.run(100.0)

        val customers = tc.traces
                .map { it.component }
                .filterNotNull().distinct()
                .filter { it.name.startsWith("Customer") }

        assertEquals(4, customers.size, "incorrect expected customer cont")
    }
}