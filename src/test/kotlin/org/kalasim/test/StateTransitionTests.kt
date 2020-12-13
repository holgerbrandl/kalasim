package org.kalasim.test

import org.junit.Test
import org.kalasim.*
import kotlin.test.assertEquals


class StateTransitionTests {

    @Test
    fun testCars() {

        val traces = mutableListOf<TraceElement>()

        Environment().apply {
            class TestCar : Component()

            addTraceListener { traceElement -> traces.add(traceElement) }

            object : Component() {
                override suspend fun SequenceScope<Component>.process() {
                    while (true) {
                        TestCar()
                        yield(hold(1.0))
                    }
                }
            }
        }.run(5.0)

//        traces.forEach { println(it) }

        // make sure multiple cars are created
        val cars = traces.mapNotNull { it.source }.distinct().filter { it.name.startsWith("TestCar") }
        assertEquals(6, cars.size, "expected cars count does not match")

        assert(traces[0].curComponent!!.name == MAIN)
        assert(traces[1].curComponent!!.name == MAIN)
        assert(traces[2].curComponent!!.name == MAIN)
    }


    @Test
    fun customProc() {
        class Customer : Component(process = Customer::doSmthg) {

            fun doSmthg() = sequence {
                print("hello")
                yield(terminate())
            }
        }

        Environment().apply {
            Customer()
        }.run(1.0)
    }

    @Test
    fun `it should correctly keep track of the state`() {
        var c: Component? = null
        createSimulation { c = Component() }.apply {
            run(10.0)
            c!!.status = ComponentState.WAITING
            run(10.0)
        }

        c!!.statusMonitor.printHistogram()
    }
}