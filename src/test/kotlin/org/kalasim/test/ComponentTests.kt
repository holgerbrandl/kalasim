package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.ComponentState
import org.kalasim.TraceCollector
import org.kalasim.misc.printThis

class ComponentTests {

    @Test
    fun `it should create components outside of an environment`() {
        Component("foo").info.printThis()
    }


    @Test
    fun `it should allow for an empty generator process`() = createTestSimulation(true) {
        val c = Component()
//        object : Component(){}

        run(20)

        c.status shouldBe ComponentState.DATA
    }

    @Test
    fun `it should yield and terminate automagically`() = createTestSimulation {
        val c = object : Component("foo") {
            override fun process() = sequence<Component> {
                hold(2)
            }
        }

        run(5)
        c.status shouldBe ComponentState.DATA
    }


    @Test
    fun `it should track status changes`() = createTestSimulation {
        val component = Component("foo")

        run(2)
        component.activate(delay = 1)
        run(2)


        component.statusMonitor.printHistogram()
    }

    @Test
    fun `it should preserve process definition after being data`() = createTestSimulation {
        // note: regression test, because initially broken
        val salabimTwin = """
            import salabim as sim


            class Customer(sim.Component):
                def process(self):
                    print("huhu")


            env = sim.Environment(trace=True)

            c = Customer()

            env.run(till=1)

            c.activate()
        """

        val c = Component()

        run(1)
        c.activate()
        run(1)

    }


    @Test
    fun `it support resume after interrupt`() = createTestSimulation {


        val tool = object : Component("tool") {
            override fun process() = sequence<Component> {
                hold(10)
                log("production finished")
            }
        }

        val mechanic = object : Component("tool") {
            override fun process() = sequence<Component> {
                hold(1)
                tool.interrupt()

                // do maintenance
                hold(2)
                tool.resume()
            }
        }

        run(20)

        tool.isData shouldBe true
        mechanic.isData shouldBe true
    }

    @Test
    fun `it should  hold on someones elses behalf`() = createTestSimulation(true) {
        val c = object : Component("other") {
            override fun process() = sequence<Component> {
                println("huhu")
                hold(1)
//                yield(getThis())
            }
        }
        val mechanic = object : Component("controller") {
            override fun process() = sequence<Component> {
                with(c) {
                    hold(1)
                }
                println("huhu2")
            }
        }

        val tc = TraceCollector().apply { addEventConsumer(this) }

        run(20)

        tc[4].source!!.name shouldBe "other"
    }
}

