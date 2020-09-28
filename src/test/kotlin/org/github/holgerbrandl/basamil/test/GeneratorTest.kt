package org.github.holgerbrandl.basamil.test

import org.apache.commons.math3.distribution.ExponentialDistribution
import org.github.holgerbrandl.basamil.*
import org.junit.Test
import kotlin.test.assertEquals

class TraceCollector : TraceListener {
    val traces = mutableListOf<TraceElement>()

    override fun processTrace(traceElement: TraceElement) {
        traces.add(traceElement)
    }
}

class GeneratorTest {

    class Customer(env: Environment) : Component(env = env){}

    @Test
    fun testCustomerGenerator() {

        val tc = TraceCollector()

        Environment().build {

            addTraceListener(tc)

            this + ComponentGenerator(iat = ExponentialDistribution(2.0), env = this, total = 4) { Customer(this) }
        }.run(100.0)

        val customers =
            tc.traces
                .map { it.component }
                .filterNotNull().distinct()
                .filter { it.name.startsWith("Customer") }

        assertEquals(4, customers.size, "incorrect expected customer cont")
    }
}