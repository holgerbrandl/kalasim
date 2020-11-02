package org.github.holgerbrandl.kalasim.test

import org.github.holgerbrandl.kalasim.*
import org.junit.Test
import kotlin.test.assertEquals


class StateTransitionTests {

    @Test
    fun testCars() {
        class TestCar : Component() {
            override suspend fun SequenceScope<Component>.process() {
                while (true) {
                    yield(hold(1.0))
                }
            }
        }

        val traces = mutableListOf<TraceElement>()


        Environment().apply {
            addTraceListener(object : TraceListener {
                override fun processTrace(traceElement: TraceElement) {
                    traces.add(traceElement)
                }
            })

            TestCar()
        }.run(5.0)

//        traces.forEach { println(it) }

        // make sure multiple cars are created
        val cars = traces.map { it.component }.filterNotNull().distinct().filter { it.name.startsWith("Car") }
        assertEquals(5, cars.size, "expected cars count does not match")

        assert(traces[0].component!!.name == MAIN)
        assert(traces[1].component!!.name == MAIN)
        assert(traces[2].component!!.name == MAIN)
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
}