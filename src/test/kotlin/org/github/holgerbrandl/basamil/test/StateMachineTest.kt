package org.github.holgerbrandl.basamil.test

import org.github.holgerbrandl.basamil.*
import org.junit.Test


class StateMachineTest {

    @Test
    fun testCars() {
        class TestCar(env: Environment) : Component(env = env) {
            override suspend fun SequenceScope<Component>.process() {
                while (true) {
                    yield(hold(1))
                }
            }
        }

        val traces = mutableListOf<TraceElement>()


        Environment().build {

            addTraceListener(object : TraceListener {
                override fun processTrace(traceElement: TraceElement) {
                    traces.add(traceElement)
                }
            })

            addComponent(TestCar(this))
            addComponent(TestCar(this))
            addComponent(TestCar(this))

            this
        }.run(5)

        traces.forEach { println(it) }

        assert(traces[0].component!!.name==MAIN)
        assert(traces[1].component!!.name==MAIN)
        assert(traces[2].component!!.name==MAIN)

    }
}