package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.kalasim.*
import org.kalasim.ComponentState.*
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.createTestSimulation
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


class StateTransitionTests {

    @Test
    fun testCars() {
        val traces = mutableListOf<Event>()

        Environment().apply {
            class TestCar : Component()

            addEventListener { traces.add(it) }

            object : Component() {
                override fun process() = sequence {
                    while(true) {
                        TestCar()
                        hold(1.minutes)
                    }
                }
            }
        }.run(5.minutes)

//        events.forEach { println(it) }

        val interactions = traces.filterIsInstance<EntityCreatedEvent>()
        // make sure multiple cars are created
        println("car events are ${interactions.joinToString("\n") { it.toString() }}")

        val cars = interactions.map { it.entity }.distinct().filter { it.name.startsWith("TestCar") }
        assertEquals(6, cars.size, "expected cars count does not match")
    }


    @OptIn(AmbiguousDuration::class)
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
    fun `it should correctly keep track of the state`() = createTestSimulation {
        val someState = State(false)

        val c = object : Component() {
            override fun process() = sequence {
                hold(10.days)
                wait(someState) { true }
            }
        }

        run(20.days)

        c.stateTimeline.printHistogram()
    }
}

class ComponentReceiverInteractionTests {

    @Test
    fun `it should passivate, cancel, hold and activate on behalf of another component`() =
        createTestSimulation {

//            Resource()
//            State("foo")

            val other = object : Component("other") {
                override fun process() =
                    sequence {
                        log("starting process!")
                        hold(100.days)

                        log("other process continued")
                        hold(100.days)
                    }
            }


            val comp = object : Component() {

                override fun process() =
                    sequence {
                        other.passivate()
                        hold(4.days)

                        other.activate()
                        hold(4.days)

                        other.cancel()
                        hold(4.days)

                        other.activate()
                        other.hold(2.days)
                        hold(4.days)
                    }
            }

            // note this is a compiler test only. the example is not meaningful
            run(1000.days)

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
