package org.github.holgerbrandl.basamil.test

import org.github.holgerbrandl.basamil.*
import org.junit.Test
import kotlin.test.assertEquals


class StateMachineTest {

    @Test
    fun testCars() {
        class TestCar(env: Environment) : Component(env = env) {
            override suspend fun SequenceScope<Component>.process() {
                while (true) {
                    yield(hold(1.0))
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
        }.run(5.0)

//        traces.forEach { println(it) }

        // make sure multiple cars are created
        val cars = traces.map{it.component}.filterNotNull().distinct().filter { it.name.startsWith("Car") }
        assertEquals(5, cars.size, "expected cars count does not match")

        assert(traces[0].component!!.name==MAIN)
        assert(traces[1].component!!.name==MAIN)
        assert(traces[2].component!!.name==MAIN)
    }


    @Test
    fun customProc(){
        class Customer(env: Environment) : Component(env, process=Customer::doSmthg){

            fun doSmthg(){
                print("hello")
                terminate()
            }
        }

        Environment().build { this + Customer(env= this) }.run(1.0)
    }
}