package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.ComponentState.*
import org.kalasim.analysis.EntityCreatedEvent
import kotlin.test.assertEquals


class StateTransitionTests {

    @Test
    fun testCars() {
        val traces = mutableListOf<Event>()

        Environment().apply {
            class TestCar : Component()

            addEventListener { traces.add(it) }

            object : Component() {
                override fun process() = sequence {
                    while (true) {
                        TestCar()
                        hold(1.0)
                    }
                }
            }
        }.run(5.0)

//        events.forEach { println(it) }

        val interactions = traces.filterIsInstance<EntityCreatedEvent>()
        // make sure multiple cars are created
        println("car events are ${interactions.map { it.toString() }.joinToString("\n")}")

        val cars = interactions.map { it.entity }.distinct().filter { it.name.startsWith("TestCar") }
        assertEquals(5, cars.size, "expected cars count does not match")
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
            c!!.componentState = WAITING
            run(10.0)
        }

        c!!.stateTimeline.printHistogram()
    }


    class ComponentReceiverInteractionTests {

        @Test
        fun `it should passivate, cancel, hold and activate on behalf of another component`() =
            createTestSimulation(true) {

                val r = Resource()
                val s = State("foo")

                val other = object : Component("other") {
                    override fun process() =
                        sequence {
                            log("starting process!")
                            hold(100)

                            log("other process continued")
                            hold(100)
                        }
                }


                val comp = object : Component() {

                    override fun process() =
                        sequence {
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

                comp.stateTimeline.statsData().values shouldBe listOf(
                    DATA, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED,
                    CURRENT, SCHEDULED, CURRENT, DATA
                )

                other.stateTimeline.statsData().values shouldBe listOf(
                    DATA, SCHEDULED, CURRENT, SCHEDULED, PASSIVE, SCHEDULED, CURRENT, SCHEDULED, DATA,
                    SCHEDULED, SCHEDULED, CURRENT, SCHEDULED, CURRENT, SCHEDULED, CURRENT, DATA
                )
            }
    }
}