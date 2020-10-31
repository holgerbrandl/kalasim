package org.github.holgerbrandl.kalasim.test

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

            fun waitForGreen() {
                wait(trafficLight, "green")
                env.printTrace("passing crossing")
                terminate()
            }
        }

        val sim = createSimulation {
            single { State("red") }
            single { Car() }
        }

        val trafficLight = sim.get<State<String>>()
//        val car = sim.get<Car>()
        sim.apply{
            trafficLight.printInfo()
            //todo assert on waiters

            run(10.0)

            // toogle state
            trafficLight.value = "green"

            run(10.0)

            //todo assert on waiters
            trafficLight.printInfo()
        }
    }
}