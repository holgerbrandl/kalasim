package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.ComponentState
import org.kalasim.Resource
import org.kalasim.misc.printThis

class ComponentTests {

    @Test
    fun `it should create components outside of an environment`() {
        Component("foo").info.printThis()
    }

    @Test
    fun `it should hold and terminate`() = createTestSimulation {
        val c = object : Component("foo"){
            override fun process()  = sequence<Component> {
                yield(hold(2))
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


        val tool = object : Component("tool"){
            override fun process() = sequence<Component> {
                yield(hold(10))
                printTrace("production finished")
            }
        }

        val mechanic = object : Component("tool"){
            override fun process() = sequence<Component> {
                yield(hold(1))
                tool.interrupt()

                // do maintenance
                yield(hold(2))
                tool.resume()
            }
        }

        run(20)

        tool.isData shouldBe true
        mechanic.isData shouldBe true
    }
}

