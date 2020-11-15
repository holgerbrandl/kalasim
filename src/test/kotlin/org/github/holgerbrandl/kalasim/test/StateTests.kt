package org.github.holgerbrandl.kalasim.test

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.yield
import org.github.holgerbrandl.kalasim.*
import org.junit.Test
import org.koin.core.get

class StateTests {

    @Test
    fun testPredicate() {
        val (state, _, predicate) = StateRequest(State("foo")) { it == "House" }
        predicate(state.value)
        predicate(state.value)

//        StateRequest(State("foo")) { listOf("bar", "test").contains(it) }
//        StateRequest(State(3.0)) { it*3 < 42 }
    }

    @Test
    fun `it should wait until a predicate is met`() {

        class Car : Component(process = Car::waitForGreen) {

            val trafficLight = get<State<String>>()

            fun waitForGreen() = sequence {
                yield(wait(trafficLight, "green"))
                yield(wait(StateRequest(trafficLight){ it == "green"}))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
            single { State("red") }
        }

        sim.apply{
            val car = Car()
            car.activate()

            val trafficLight = get<State<String>>()

            trafficLight.printInfo()

            run(10.0)

            trafficLight.info.waiters.size shouldBe  1

            // toogle state
            trafficLight.value = "green"

            run(10.0)

            trafficLight.printInfo()

            trafficLight.info.waiters.shouldBeEmpty()
        }
    }


    @Test
    fun `it should wait until multiple predicates are honored`() {

        class Car : Component() {

            val trafficLight = get<State<String>>()
            val engine = get<State<Boolean>>()

            override suspend fun ProcContext.process() {
                yield(wait(trafficLight turns "green", engine turns true, all = true))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
            single { State("red") }
            single { State(false) }
        }

        sim.apply{
            val car = Car()

            val trafficLight = get<State<String>>()
            val engine = get<State<Boolean>>()

            trafficLight.printInfo()

            run(10.0)

            trafficLight.info.waiters.size shouldBe  1

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