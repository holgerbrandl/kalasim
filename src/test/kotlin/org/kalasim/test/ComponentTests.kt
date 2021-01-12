package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
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
    fun `it should log to the console`() = captureOutput {

        createTestSimulation(true) {
            object : Component("tester") {
                override fun process() = sequence<Component> {
                    wait(State(true), true)
                    hold(1)
                    request(Resource()) {
                        hold(1)
                    }
                }
            }

            run(5)
        }
    }.stdout shouldBe """
time      current               receiver              action                                       info                               
--------- --------------------- --------------------- -------------------------------------------- ----------------------------------
.00                             main                  create
.00       main
.00                             tester                create
.00                                                   activate                                     scheduled for .00
.00                             main                  run +5.00                                    scheduled for 5.00
.00       tester                tester
.00                                                   Entering waiters of State.1
.00                                                   removed from waiters of State.1
.00                                                   wait                                         scheduled for .00
.00
.00                                                   hold +1.00                                   scheduled for 1.00
1.00
1.00                            Resource.1            Created                                      capacity=1
1.00                            tester                Entering requesters of Resource.1
1.00                                                  Requesting 1.0 from Resource.1 with prio...
1.00                                                  Claimed 1.0 from 'tester'
1.00                                                  Entering claimers of Resource.1
1.00                                                  request honor Resource.1                     scheduled for 1.00
1.00
1.00                                                  hold +1.00                                   scheduled for 2.00
2.00
2.00                                                  Released 1.0 from 'tester'
2.00                                                  leaving claimers of Resource.1
2.00                                                  ended
5.00      main                  main
""".trimIndent()


    @Test
    fun `it should yield and terminate automagically`() = createTestSimulation {
        val c = object : Component("foo") {
            override fun process() = sequence {
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

        val tc = TraceCollector().apply { addEventListener(this) }

        run(20)

        tc[4].source!!.name shouldBe "other"
    }
}

