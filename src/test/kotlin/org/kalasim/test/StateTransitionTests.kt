package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*
import org.kalasim.ComponentState.*
import kotlin.test.assertEquals


class StateTransitionTests {

    @Ignore
    @Test
    fun testCars() {
        val traces = mutableListOf<Event>()

        Environment().apply {
            class TestCar : Component()

            addEventConsumer { traces.add(it) }

            object : Component() {
                override fun process() = sequence {
                    while (true) {
                        TestCar()
                        hold(1.0)
                    }
                }
            }
        }.run(5.0)

//        traces.forEach { println(it) }

        // make sure multiple cars are created
        println("car traces are ${traces.map { it.toString() }}")
        val cars = traces.mapNotNull { it.source }.distinct().filter { it.name.startsWith("TestCar") }
        assertEquals(6, cars.size, "expected cars count does not match")

        assert(traces[0].curComponent!!.name == MAIN)
        assert(traces[1].curComponent!!.name == MAIN)
        assert(traces[2].curComponent!!.name == MAIN)
    }


    @Test
    fun customProc() {
        class Customer : Component(process = Customer::doSmthg) {

            fun doSmthg() = sequence<Component> {
                print("hello")
//                terminate()
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
            c!!.status = WAITING
            run(10.0)
        }

        c!!.statusMonitor.printHistogram()
    }


    class ComponentReceiverInteractionTests {

        @Test
        fun `it should passivate, cancel, hold and activate on behalf of another component`() =
            createTestSimulation(true) {

                val r = Resource()
                val s = State<String>("foo")

                val other = object : Component("other") {
                    override fun process() = sequence<Component> {
                        log("starting process!")
                        hold(100)

                        log("other process continued")
                        hold(100)
                    }
                }


                val comp = object : Component() {

                    override fun process() = sequence<Component> {
                        other.passivate()
                        hold(4)

                        other.activate()
                        hold(4)

                        other.cancel()
                        hold(4)

                        other.activate()
                        other.hold(2)
                        hold(4)
                    }
                }

                // note this is a compiler test only. the example is not meaningful
                run(1000)

                comp.statusMonitor.statsData().values shouldBe listOf(
                    DATA, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED,
                    CURRENT, SCHEDULED, CURRENT, DATA
                )

                other.statusMonitor.statsData().values shouldBe listOf(
                    DATA, SCHEDULED, CURRENT, SCHEDULED, PASSIVE, SCHEDULED, CURRENT, SCHEDULED, DATA,
                    SCHEDULED, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED, CURRENT, DATA
                )
            }
    }
}