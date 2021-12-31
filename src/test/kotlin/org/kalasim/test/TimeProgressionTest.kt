package org.kalasim.test

import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.kalasim.*

class TimeProgressionTest {

    @Test
    fun `time should progress along with simulation`() {
        val tr = EventLog()

        class Car : Component() {
            //            override suspend fun SequenceScope<Component>.process(it: Component) {
            override fun process() = sequence {

                while (true) {
                    // wait for 1 sec
                    hold(1.0)
                    // and terminate it
//                    terminate()
                }
            }
        }

        Environment().apply {
            addEventListener(tr)

            Car()

            run(5.0)

            now shouldNotBe 0.0
        }
    }
}