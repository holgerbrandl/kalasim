package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.ComponentState.DATA
import org.kalasim.Priority.Companion.IMPORTANT
import org.kalasim.analysis.InteractionEvent
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.createTestSimulation
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@Suppress("OPT_IN_USAGE")
class QueueTests {

    @Test
    fun testWaitingLine() {
        class Foo : Component()

        val waitingLine by lazy {
            ComponentQueue<Foo>()
        }

        val env = createSimulation {
            enableComponentLogger()
            dependency { waitingLine }

            enableEventLog()
        }

        waitingLine.add(Foo())
        waitingLine.add(Foo())
        waitingLine.add(Foo())

        assertEquals(3, waitingLine.size, "expected 3 items in queue")

        // add a consumer
        object : TickedComponent() {
            override fun process() = sequence {
                while(waitingLine.isNotEmpty()) {
                    waitingLine.poll()
                    // wait for it...
                    hold(5.0)
                }

                passivate()
            }
        }

        env.run(50.0)

        assertEquals(0, waitingLine.size, "expected empty queue")
    }


    @Test
    fun `it should correctly schedule same-time events`() {
//        import salabim as sim
//
//        class Customer(sim.Component):
//            def process(self):
//                print("huhu from " + self._name)
//
//
//        env = sim.Environment(trace=True)
//
//        Customer(name="Car1", at=3)
//        Customer(name="Car2", at=3)
//
//        env.run(till=5)
//
//        print("done")


        // also see https://simpy.readthedocs.io/en/latest/topical_guides/time_and_scheduling.html

        createSimulation {
            enableComponentLogger()

//            class TestComponent(name:String, at: Number): Component(name){
//                override fun process(): Sequence<Component> = super.process()
//            }

            val c1 = object : Component("comp1", at = startDate + 3.minutes) {
                override fun process() = sequence<Component> {}
            }
            val c2 = object : Component("comp2", at = startDate + 3.minutes) {
                override fun process() = sequence<Component> {}
            }


            val tc = EventLog().also { addEventListener(it) }


            queue.first() shouldBe c1
            queue.last() shouldBe c2

            run(10)

            tc.events.filterIsInstance<InteractionEvent>()
                .filter { it.action == "Ended" }
                .apply {
                    size shouldBe 2
                    get(0).component?.name shouldBe c1.name
                }
        }

        // redo but with priority
        createSimulation {
            enableComponentLogger()

            // to make sure that the component methods are auto-scheduled we need to overrride them
//            class TestComponent(name:String, at: Number, priority: Priority = NORMAL): Component(name, priority = priority){
//                override fun process(): Sequence<Component> = super.process()
//            }

            val c1 = object : Component("comp1", at = startDate + 3.minutes) {
                override fun process() = sequence<Component> {}
            }
            val c2 = object : Component("comp2", at = startDate + 3.minutes, priority = IMPORTANT) {
                override fun process() = sequence<Component> {}
            }
            val tc = EventLog().also { addEventListener(it) }


            queue.first() shouldBe c2
            queue.last() shouldBe c1

            run(10)
        }
    }


    @Test
    fun `it should correctly calculate inversed iat`() = createTestSimulation {
        run(5)
        val inverseIat = inversedIatDist(6, 7, 9)

        run(inverseIat())
        now shouldBe asSimTime(6)

        run(inverseIat())
        now shouldBe asSimTime(7)

        run(inverseIat())
        now shouldBe asSimTime(9)

        // since the underying iterator is exhausted now, we expect an exception
        shouldThrow<NoSuchElementException> {
            inverseIat()
        }
    }

    @Test
    fun `it should correctly generate using inversed iat`() = createTestSimulation {
        val arrivalsTimes = listOf(6, 7, 9)
        val inverseIat = inversedIatDist(*arrivalsTimes.toTypedArray())

        val generated = mutableListOf<Component>()
        ComponentGenerator(inverseIat) { Component() }.addConsumer { generated.add(it) }

        run(20)
        generated.map { it.creationTime.toTickTime().value.roundToInt() } shouldBe arrivalsTimes
    }

    @OptIn(AmbiguousDuration::class)
    @Test
    fun `it should form batches`() = createTestSimulation {
        // see ferryman.md
        class Passenger : Component()

        val fm = object : Component() {
            val waitingLine = ComponentQueue<Passenger>()

            override fun process() = sequence {
                val batchComplete = batch(waitingLine, 4, timeout = 10.minutes)
                batchComplete.size shouldBe 4
                env.nowTT shouldBe 8.tt

                hold(until = TickTime(20.0).asSimTime())

                val batchPartial = batch(waitingLine, 4, timeout = 10.minutes)
                batchPartial.size shouldBe 2
                env.now shouldBe asSimTime(30)
            }
        }

        ComponentGenerator(inversedIatDist(1, 4, 5, 8, 21, 25)) { Passenger() }
            .addConsumer { fm.waitingLine.add(it) }

        run(100)

        fm.componentState shouldBe DATA
    }

    @Test
    fun `batch creation should not timeout by default`() = createTestSimulation {
        class Passenger : Component()

        val fm = object : Component() {
            val waitingLine = ComponentQueue<Passenger>()

            override fun process() = sequence {
                val batchComplete = batch(waitingLine, 4)
                batchComplete.size shouldBe 4
                env.nowTT shouldBe 50.tt
            }
        }

        ComponentGenerator(inversedIatDist(1, 4, 5, 50, 60, 70)) { Passenger() }
            .addConsumer { fm.waitingLine.add(it) }

        run(55)

        fm.componentState shouldBe DATA
    }
}
