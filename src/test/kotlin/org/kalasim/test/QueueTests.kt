package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.ComponentState.*
import kotlin.math.roundToInt
import kotlin.test.assertEquals

class QueueTests {

    @Test
    fun testWaitingLine() {
        class Foo : Component()

        val waitingLine by lazy {
            ComponentQueue<Foo>()
        }

        val env = configureEnvironment(true) {
            add { waitingLine }
        }

        env.addEventListener(TraceCollector())

        waitingLine.add(Foo())
        waitingLine.add(Foo())
        waitingLine.add(Foo())

        assertEquals(3, waitingLine.size, "expected 3 items in queue")

        // add a consumer
        object : Component() {
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

        createSimulation(true) {
            val c1 = Component("comp1", at = 3)
            val c2 = Component("comp2", at = 3)

            val tc = TraceCollector().also { addEventListener(it) }


            queue.first() shouldBe c1
            queue.last() shouldBe c2

            run(10)

            tc.traces.filter { it.renderAction() == "Ended" }.apply {
                size shouldBe 2
                get(0).source?.name shouldBe c1.name
            }
        }

        // redo but with priority
        createSimulation(true) {
            val c1 = Component("comp1", at = 3)
            val c2 = Component("comp2", at = 3, priority = 3)

            val tc = TraceCollector().also { addEventListener(it) }


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
        now shouldBe 6

        run(inverseIat())
        now shouldBe 7

        run(inverseIat())
        now shouldBe 9

        // since the underying iterator is exhausted now, we expect an exception
        shouldThrow<NoSuchElementException> {
            inverseIat()
        }
    }

   @Test
    fun `it should correctly generate using inversed iat`() = createTestSimulation {
        val arrivalsTimes = listOf(6, 7, 9)
        val inverseIat = inversedIatDist(*arrivalsTimes.toTypedArray())

       val generated  = mutableListOf<Component>()
       ComponentGenerator(inverseIat){ Component() }.addConsumer{ generated.add(it)}

       run(20)
       generated.map{ it.creationTime.roundToInt()} shouldBe arrivalsTimes
    }

    @Test
    fun `it should form batches`() = createTestSimulation {
        // see ferryman.md
        class Passenger : Component(process = null)

        val fm = object : Component() {
            val waitingLine = ComponentQueue<Passenger>()

            override fun process() = sequence {
                val batchComplete = batch(waitingLine, 4, timeout = 10)
                batchComplete.size shouldBe 4
                env.now shouldBe 8

                hold(till = 20)

                val batchPartial = batch(waitingLine, 4, timeout = 10)
                batchPartial.size shouldBe 2
                env.now shouldBe 30
            }
        }

        ComponentGenerator(inversedIatDist(1, 4, 5, 8, 21, 25)) { Passenger() }
            .addConsumer { fm.waitingLine.add(it) }

        run(100)

        fm.status shouldBe DATA
    }
}
