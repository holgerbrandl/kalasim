package org.github.holgerbrandl.desimuk.test

import org.github.holgerbrandl.desimuk.Component
import org.github.holgerbrandl.desimuk.Environment
import org.junit.Test

class TimeProgressionTest {

    @Test
    fun `time should progress along with simulatio`(){
        val tr = TraceCollector()

        class Car : Component() {
            override suspend fun SequenceScope<Component>.process(it: Component) {
                while (true) {
                    // wait for 1 sec
                    yield(hold(1.0))
                    // and terminate it
                    yield(terminate())
                }
            }
        }

        Environment().apply {
            addTraceListener(tr)

            Car()
        }.run(5.0)


        //todo assert that time is progressings
    }
}