package org.kalasim.test

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*
import org.koin.core.component.get

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
    fun `it should wait until a predicate is met`() {

        class Car : Component(process = Car::waitForGreen) {

            val trafficLight = get<State<String>>()

            fun waitForGreen() = sequence {
                yield(wait(trafficLight, "green"))
                val stateRequest: StateRequest<String> = StateRequest(trafficLight) { it == "green" }
                val (state: State<String>, bar: Int?, predicate: (String) -> Boolean) = stateRequest
                yield(wait(stateRequest))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
            single { State("red") }
        }

        sim.apply {
            val car = Car()
            car.activate()

            val trafficLight = get<State<String>>()

            trafficLight.printInfo()

            run(10.0)

            trafficLight.info.waiters.size shouldBe 1

            // toggle state
            trafficLight.value = "green"

            run(10.0)

            trafficLight.printInfo()

            trafficLight.info.waiters.shouldBeEmpty()
        }
    }

    @Test
    fun `it should wait until multiple predicates are honored`() {

        class TrafficLight : State<String>("red")
        class Engine : State<Boolean>(false)

        class Car : Component() {

            val trafficLight = get<TrafficLight>()
            val engine = get<Engine>()

            override suspend fun ProcContext.process() {
                yield(wait(trafficLight turns "green", engine turns true, all = true))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
            single { TrafficLight() }
            single { Engine() }
        }

        val car = Car()

        val trafficLight = sim.get<TrafficLight>()
        val engine = sim.get<Engine>()

        trafficLight.printInfo()

        sim.run(10.0)

        trafficLight.info.waiters.size shouldBe 1

        // toggle state
        trafficLight.value = "green"

        sim.run(10.0)

        trafficLight.printInfo()

        trafficLight.info.waiters.size shouldBe 1

        car.isWaiting shouldBe true

        // now honor the engine
        engine.value = true

        car.printInfo()

        sim.run(10.0)

        car.isWaiting shouldBe false
        car.isData shouldBe true

        trafficLight.info.waiters.shouldBeEmpty()
        engine.info.waiters.shouldBeEmpty()
    }


    @Test
    @Ignore("because its unclear how to nicely")
// https://kotlinlang.slack.com/archives/C67HDJZ2N/p1607195460178600
// D:\projects\misc\koin_test\src\main\kotlin\com\github\holgerbrandl\Test.kt
    fun `resolve and honor multiple predicates without subclassing`() {

        class Car : Component() {

            //            val trafficLight = get<State<String>>(TypeQualifier(String::class))
//            val engine = get<State<Boolean>>(TypeQualifier(Boolean::class))
            val trafficLight = get<State<String>>()
            val engine = get<State<Boolean>>()

            override suspend fun ProcContext.process() {
                yield(wait(trafficLight turns "green", engine turns true, all = true))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
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

            trafficLight.info.waiters.size shouldBe 1

            // toogle state
            trafficLight.value = "green"

            run(10.0)

            trafficLight.printInfo()

            trafficLight.info.waiters.shouldBeEmpty()

            car.isWaiting shouldBe true

            // now honor the engine
            engine.value = true

            car.printInfo()
            car.isWaiting shouldBe false
            car.isData shouldBe true
        }
    }
}
