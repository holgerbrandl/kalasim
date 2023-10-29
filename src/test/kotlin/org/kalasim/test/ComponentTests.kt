@file:OptIn(AmbiguousDuration::class)

package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*
import org.kalasim.ComponentState.DATA
import org.kalasim.ComponentState.SCHEDULED
import org.kalasim.analysis.InteractionEvent
import org.kalasim.misc.*
import kotlin.test.fail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit


internal class NoOpComponent(name: String? = null) : Component(name) {
    override fun process() = sequence<Component> {
        println("Hello from  $name")
    }
}

class ComponentTests {

    @Test
    fun `it assign sensible names to anonymous object components`() = createTestSimulation {
        val c = object : Component() {}

        c.name shouldBe "Component.1"
    }

    @Test
    fun `it should  auto-indexing if the name ends with a comma or dot`() = createTestSimulation {
        val c1 = object : Component("Foo") {}
        val c2 = object : Component("Bar.") {}
        val c3 = object : Component("Bar.") {}
        val c4 = object : Component("Bla-") {}
        val c5 = object : Component("Bla-") {}

        c1.name shouldBe "Foo"
        c2.name shouldBe "Bar.1"
        c3.name shouldBe "Bar.2"
        c4.name shouldBe "Bla-1"
        c5.name shouldBe "Bla-2"
    }


    @Test
    fun `it should not create components outside of an environment`() {
        DependencyContext.stopKoin()

        shouldThrow<MissingDependencyContextException> {
            Component("foo")
        }
    }

    @Test
    fun `components should be in DATA by default unless a process is defined`() = createTestSimulation {

//        object : Component() {
//            override fun process() = sequence<Component> {
//                println() // just needed to prevent IDE pruning of overriding method
//            }
//        }.componentState shouldBe SCHEDULED


        Component("foo").componentState shouldBe DATA

        Component("foo", process = Component::process).componentState shouldBe DATA


    }


    @Test
    fun `it should capture component state with snapshot`() = createTestSimulation {
        val info = Component("foo").snapshot
        run(10.days)

        info.status shouldBe DATA
        info.scheduledTime shouldBe null

        info.toString() shouldBeDiff """
            {
              "scheduledTime": null,
              "creationTime": "1970-01-01T00:00:00Z",
              "now": "1970-01-01T00:00:00Z",
              "name": "foo",
              "claims": {},
              "requests": {},
              "status": "DATA"
            }""".trimIndent()
    }


    @Test
    fun `it should reactivate a custom process definition after being DATA`() = createTestSimulation {
        var counter = 0

        class MyComponent : Component(process = MyComponent::myProcess) {
            fun myProcess() = sequence {
                hold(1.day)
                println("hello from $name")

                isCurrent shouldBe true

                counter++
            }
        }

        val c = MyComponent()

        c.componentState shouldBe SCHEDULED

        run(2.days)
        counter shouldBe 1
        c.componentState shouldBe DATA

        c.activate(delay = 1.day)
        c.componentState shouldBe SCHEDULED

        run()

        c.componentState shouldBe DATA
        counter shouldBe 2
    }


    @Test
    fun `it should allow to run a cyclic process`() = createTestSimulation {
        var repeatCounter = 0
        object : Component() {
            override fun repeatedProcess() = sequence {
                hold(1.days)
                repeatCounter++
            }
        }

        run(8.days)

        repeatCounter shouldBe 7
    }


    @Test
    fun `it should allow for an empty generator process`() = createTestSimulation {

        val c = Component()
//        object : Component(){}

        run(20)

        c.componentState shouldBe DATA
    }


    @Test
    fun `it must enforce a process definition when using at`() = createTestSimulation {
        shouldThrow<IllegalArgumentException> {
            Component("comp1", at = now + 3.minutes)
        }
    }

    @Test
    fun `it shall allow suppress automatic activation of a process definition`() = createTestSimulation {
        object : Component(process = Component::none) {
            override fun process(): Sequence<Component> {
                fail()
            }
        }

        run()
    }

    @Test
    fun `it shall not allow self-activation with activate()`() = createTestSimulation {
        object : Component() {

            override fun process() = sequence {
                hold(1.days)
                activate()
                fail("it must not get here")
            }

        }

        shouldThrow<IllegalArgumentException> {
            run()
        }
    }

    @Test
    fun `it shall activate a sub-process with activate()`() =
        createTestSimulation(tickDurationUnit = DurationUnit.DAYS) {
            class MultiProcessor : Component() {
                var sp1Started = false
                var sp2Started = false

                override fun process() = sequence {
                    hold(1.days)
                }

                fun subProcess1(): Sequence<Component> = sequence {
                    sp1Started = true
                    hold(1.days)
                    activate(process = MultiProcessor::subProcess2)

                    fail("it should never get here")
                }

                fun subProcess2(): Sequence<Component> = sequence {
                    sp2Started = true
                    hold(1.days)
                    activate(process = MultiProcessor::subProcess1)

                    fail("it should never get here")
                }
            }

            val mp = MultiProcessor()

            run()

            now shouldBe asSimTime(1)

            // activate sub-process
            mp.activate(process = MultiProcessor::subProcess1)

            run(10.days)

            mp.sp1Started shouldBe true
            mp.sp2Started shouldBe true

        }

    @Test
    fun `it shall consume a sub-process inplace`() = createTestSimulation(tickDurationUnit = DurationUnit.DAYS) {
        class MultiProcessor : Component() {
            var sp1Started = false

            override fun process() = sequence {
                hold(1.days)
                yieldAll(subProcess1())
                hold(1.days)
            }

            fun subProcess1(): Sequence<Component> = sequence {
                sp1Started = true
                hold(1.days)
            }
        }

        val mp = MultiProcessor()

        run()

        now shouldBe asSimTime(3)
        mp.sp1Started shouldBe true
    }


    @Test
    fun `it should log to the console`() = captureOutput {

        createTestSimulation {
            object : Component("tester") {
                override fun process() = sequence {
                    wait(State(true), true)
                    hold(1.minute)
                    request(Resource()) {
                        hold(1.minute)
                    }
                }
            }

            run(5.minutes)
        }
    }.stdout shouldBeDiff """time      current               receiver              action                                                 info                               
--------- --------------------- --------------------- ------------------------------------------------------ ----------------------------------
.00                             tester                Created
.00                                                   Activated, scheduled for .00                           New state: scheduled
.00                             main                  Running; Hold +5.00, scheduled for 5.00                New state: scheduled
.00       tester                State.1               Created                                                Initial value: true
.00                             tester                Waiting, scheduled for .00                             New state: scheduled
.00                                                   Hold +1.00, scheduled for 1.00                         New state: scheduled
1.00                            Resource.1            Created                                                capacity=1
1.00                            tester                Requested 1.0 from 'Resource.1'
1.00                                                  Claimed 1.0 from 'Resource.1'
1.00                                                  Request honored by Resource.1; Activated, schedule... New state: scheduled
1.00                                                  Hold +1.00, scheduled for 2.00                         New state: scheduled
2.00                                                  Released 1.0 from 'Resource.1'
2.00                                                  ResourceActivityEvent(requested=1970-01-01T00:01:0...
2.00                                                  Ended                                                  New state: data""".trimIndent()


    @Test
    fun `it should yield and terminate automagically`() = createTestSimulation {
        val c = object : Component("foo") {
            override fun process() = sequence {
                hold(2.days)
            }
        }

        run(5.days)
        c.componentState shouldBe DATA
    }


    @Ignore
    @Test
    fun `it should allow to disable interaction logging`() = createTestSimulation {
        entityTrackingDefaults.disableAll()

        val r = Resource()//.apply {  trackingPolicy = ResourceTrackingConfig(logClaimRelease = false ) }
        val s: State<String> = State("foo")

        object : Component("foo") {

            override fun process() = sequence {
                hold(2.days)

                s.value = "bar"

                request(r) {
//                                    s.value = "bar"
                }

                log("work done")
            }
        }

        val tc = enableEventLog()

        run(10)

        tc.events.apply {
            size shouldBe 2
            last().shouldBeInstanceOf<InteractionEvent>()
            (last() as InteractionEvent).action shouldBe "work done"
        }

//        InternalMetricsTrackingDefaults.enab()

    }


    @Test
    fun `it should enforce that either hold or until is not null in hold`() = createTestSimulation {
        object : Component("foo") {
            override fun process() = sequence {
                hold(until = null as SimTime?)
                fail("it should not allow calling hold with duration and until being both null ")
            }
        }

        shouldThrow<IllegalArgumentException> {
            run(5)
        }
    }


    @Test
    fun `it should track status changes`() = createTestSimulation {
        val component = NoOpComponent("foo")

        run(2.days)
        component.activate(delay = 1.days)
        run(2.days)

        component.stateTimeline.printHistogram()
    }

    @Test
    fun `it should preserve process definition after being data`() = createTestSimulation {
        // note: regression test, because initially broken
        @Suppress("UNUSED_VARIABLE") val salabimTwin = """
            import salabim as sim


            class Customer(sim.Component):
                def process(self):
                    print("huhu")


            env = sim.Environment(trace=True)

            c = Customer()

            env.run(till=1)

            c.activate()
        """

        val c = NoOpComponent("foo")

        run(1)
        c.activate()
        run(1)
    }


    @Test
    fun `it support resume after interrupt`() = createTestSimulation {
        val tool = object : Component("tool") {
            override fun process() =
                sequence {
                    hold(10.days)
                    log("production finished")
                }
        }

        val mechanic = object : Component("tool") {
            override fun process() =
                sequence {
                    hold(1.days)
                    tool.interrupt()

                    // do maintenance
                    hold(2.days)
                    tool.resume()
                }
        }

        run(20.days)

        tool.isData shouldBe true
        mechanic.isData shouldBe true
    }

    @Test
    // https://github.com/salabim/salabim/issues/24
    fun `all interactions should fail for an interrupted component`() = createTestSimulation {
        val tool = object : Component("tool") {
            override fun process() =
                sequence {
                    hold(10.days)
                    log("production finished")
                }
        }

        val mechanic = object : Component("tool") {
            override fun process() =
                sequence {
                    hold(1.days)
                    tool.interrupt()

                    hold(1.days)
                    shouldThrow<IllegalArgumentException> {
                        tool.hold(1.days)
                    }

                    // do maintenance
                    hold(2.days)
                    tool.resume()

                    tool.hold(1.days)

                }
        }

        run(20.days)

        tool.isData shouldBe true
        mechanic.isData shouldBe true
    }


    @OptIn(AmbiguousDurationComponent::class)
    @Test
    fun `it should interrupt and resume a passive component`() = createTestSimulation {
        val tool = object : Component("tool") {
            override fun process() = sequence {
                passivate()
            }
        }
        run(1)


        object : TickedComponent("interrupter") {
            override fun process() = sequence {
                tool.interrupt()

                // do maintenance
                hold(2)

                tool.resume()
            }
        }

        run(10)

        tool.isPassive shouldBe true
    }

    @Test
    fun `it should  hold on someones elses behalf`() = createTestSimulation {

        val c = object : Component("other") {
            override fun process() =
                sequence {
                    println("huhu")
                    hold(1.minutes)
//                yield(getThis())
                }
        }

        object : Component("mechanic") {
            override fun process() =
                sequence<Component> {
                    with(c) {
                        hold(1.minutes)
                    }
                    println("huhu2")
                }
        }

        val tc = EventLog().apply { addEventListener(this) }

        run(20)

        (tc[4] as InteractionEvent).component!!.name shouldBe "other"
    }


    @Test
    fun `it should throw user exceptions`() = createTestSimulation {

        class MyException(msg: String) : IllegalArgumentException(msg)

        object : Component("other") {
            override fun process() =
                sequence {

                    hold(1.minute)

                    throw MyException("something went wrong")
                }
        }

        shouldThrow<MyException> { run(10.minutes) }
    }
}


