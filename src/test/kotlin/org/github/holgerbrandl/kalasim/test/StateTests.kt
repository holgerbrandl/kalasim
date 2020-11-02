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
        val (state, _, predicate) = StateRequest(State("foo")) { it.value == "House" }
        predicate(state)
    }

    @Test
    fun `it should wait until a predicate is met`() {

        class Car : Component(process = Car::waitForGreen) {

            val trafficLight = get<State<String>>()

            fun waitForGreen() = sequence {
                yield(wait(trafficLight, "green"))
                printTrace("passing crossing")
                yield(terminate())
            }
        }

        val sim = configureEnvironment {
            single { State("red") }
        }

        sim.apply{
            Car()

            val trafficLight = get<State<String>>()

            trafficLight.printInfo()
            //todo assert on waiters

            run(10.0)

            trafficLight.info.waiters.size shouldBe  1

            // toogle state
            trafficLight.value = "green"

            run(10.0)

            //todo assert on waiters
            trafficLight.printInfo()

            trafficLight.info.waiters.shouldBeEmpty()
        }
    }
}