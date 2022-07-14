package org.kalasim.test

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import junit.framework.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*

class StateTests {

    @Test
    fun testPredicate() {
        createSimulation {
            val (state, _, predicate) = StateRequest(State("foo")) { it == "House" }
            predicate(state.value)
            predicate(state.value)
        }

//        StateRequest(State("foo")) { listOf("bar", "test").contains(it) }
//        StateRequest(State(3.0)) { it*3 < 42 }
    }

    @Test
    fun `it should gracefully print an empty state`() {
        createSimulation {
            State("foo").apply {
                println(snapshot)
                println(toString())
                printHistograms()
            }
        }

//        StateRequest(State("foo")) { listOf("bar", "test").contains(it) }
//        StateRequest(State(3.0)) { it*3 < 42 }
    }

    @Test
    fun `it should wait until a predicate is met`() {

        class Car : Component(process = Car::waitForGreen) {

            val trafficLight = get<State<String>>()

            fun waitForGreen() = sequence {
                // just to increase test-coverage we use a predicate here
                wait(trafficLight, triggerPriority = Priority.IMPORTANT) { it == "green" }

                val stateRequest: StateRequest<String> = StateRequest(trafficLight) { it == "green" }
//                val (state: State<String>, bar: Int?, predicate: (String) -> Boolean) = stateRequest

                wait(stateRequest)
                log("passing crossing")
            }
        }

        val sim = configureEnvironment(true) {
            single { State("red") }
        }

        sim.apply {
            val car = Car()
            car.activate()

            val trafficLight = get<State<String>>()

            trafficLight.printInfo()

            run(10.0)

            trafficLight.snapshot.waiters.size shouldBe 1

            // toggle state
            trafficLight.value = "green"

            run(10.0)

            trafficLight.printInfo()

            trafficLight.snapshot.waiters.shouldBeEmpty()
        }
    }

    private enum class TestColor { RED, GREEN }

    @Test
    fun `it trigger once and keep the other waiters`() {
        captureOutput {
            createTestSimulation(true) {
                val state = State(TestColor.RED)

                var wasAStarted = false

                object : Component("A") {
                    override fun process() = sequence {
                        wasAStarted = true
                        wait(state, TestColor.GREEN)
                        fail()
                    }
                }

                var wasBHonored = false
                object : Component("B") {
                    override fun process() = sequence {
                        wait(state, TestColor.GREEN, triggerPriority = Priority.IMPORTANT)
                        wasBHonored = true
                    }
                }

                run(1)
                state.trigger(TestColor.GREEN, max = 1)
                run(1)

                wasAStarted shouldBe true
                wasBHonored shouldBe true

            }
        }.stdout shouldBeDiff """
            time      current               receiver              action                                                 info                               
            --------- --------------------- --------------------- ------------------------------------------------------ ----------------------------------
            .00                             main                  Created
            .00                             State.1               Created                                                Initial value: RED
            .00                             A                     Created
            .00                                                   Activated, scheduled for .00                           New state: scheduled
            .00                             B                     Created
            .00                                                   Activated, scheduled for .00                           New state: scheduled
            .00                             main                  Running; Hold +1.00, scheduled for 1.00                New state: scheduled
            .00       A                     A                     Waiting, scheduled for <inf>                           New state: scheduled
            .00       B                     B                     Waiting, scheduled for <inf>                           New state: scheduled
            1.00      main                                        State changes to 'GREEN' with trigger allowing 1 c...
            1.00                            B                     Waiting, scheduled for 1.00                            New state: scheduled
            1.00                                                  State changed to 'RED'
            1.00                            main                  Running; Hold +1.00, scheduled for 2.00                New state: scheduled
            1.00      B                     B                     Ended                                                  New state: data
        """.trimIndent()
    }

    @Test
    fun `it should wait until multiple predicates are honored`() {

        class TrafficLight : State<String>("red")
        class Engine : State<Boolean>(false)

        class Car : Component() {

            val trafficLight = get<TrafficLight>()
            val engine = get<Engine>()

            override fun process() = sequence {
                wait(trafficLight turns "green", engine turns true, all = true)
                log("passing crossing")
//                terminate()
            }
        }

        val sim = configureEnvironment(true) {
            single { TrafficLight() }
            single { Engine() }
        }

        val car = Car()

        val trafficLight = sim.get<TrafficLight>()
        val engine = sim.get<Engine>()

        trafficLight.printInfo()

        sim.run(10.0)

        trafficLight.snapshot.waiters.size shouldBe 1

        // toggle state
        trafficLight.value = "green"

        sim.run(10.0)

        trafficLight.printInfo()

        trafficLight.snapshot.waiters.size shouldBe 1

        car.isWaiting shouldBe true

        // now honor the engine
        engine.value = true

        car.printInfo()

        sim.run(10.0)

        car.isWaiting shouldBe false
        car.isData shouldBe true

        trafficLight.snapshot.waiters.shouldBeEmpty()
        engine.snapshot.waiters.shouldBeEmpty()
    }


    @Test
    @Ignore("Because its unclear how to do this nicely. The workaround it to use named koin-entities")
    // https://kotlinlang.slack.com/archives/C67HDJZ2N/p1607195460178600
    // https://github.com/InsertKoinIO/koin/issues/976
    fun `resolve generic parameters and honor multiple predicates without subclassing`() {

        class Car : Component() {

            //            val trafficLight = get<State<String>>(TypeQualifier(String::class))
//            val engine = get<State<Boolean>>(TypeQualifier(Boolean::class))
            val trafficLight = get<State<String>>()
            val engine = get<State<Boolean>>()

            override fun process() = sequence {
                wait(trafficLight turns "green", engine turns true, all = true)
                log("passing crossing")
//                terminate()
            }
        }

        val sim = configureEnvironment(true) {
//            single(TypeQualifier(String::class)) { State("red") }
//            single(TypeQualifier(Boolean::class)) { State(false) }
            single { State("red") }
            single { State(false) }
        }

        sim.apply {
            val car = Car()

            val trafficLight = get<State<String>>()
            val engine = get<State<Boolean>>()

            trafficLight.printInfo()

            run(10.0)

            trafficLight.snapshot.waiters.size shouldBe 1

            // toggle state
            trafficLight.value = "green"

            run(10.0)

            trafficLight.printInfo()

            trafficLight.snapshot.waiters.shouldBeEmpty()

            car.isWaiting shouldBe true

            // now honor the engine
            engine.value = true

            car.printInfo()
            car.isWaiting shouldBe false
            car.isData shouldBe true
        }
    }
}
