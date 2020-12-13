package org.kalasim.test

import org.junit.Test
import org.kalasim.Component
import org.kalasim.ComponentQueue
import org.kalasim.add
import org.kalasim.configureEnvironment
import kotlin.test.assertEquals

class QueueTests {

    @Test
    fun testWaitingLine() {
        class Foo : Component()

        val waitingLine by lazy {
            ComponentQueue<Foo>()
        }

        val env = configureEnvironment(true) {
            add { waitingLine }
        }

        env.addTraceListener(TraceCollector())

        waitingLine.add(Foo())
        waitingLine.add(Foo())
        waitingLine.add(Foo())

        assertEquals(3, waitingLine.size, "expected 3 items in queue")

        // add a consumer
        object : Component() {
            override suspend fun SequenceScope<Component>.process(it: Component) {
                while (waitingLine.isNotEmpty()) {
                    waitingLine.poll()
                    // wait for it...
                    yield(hold(5.0))
                }

                yield(it.passivate())
            }
        }

        env.run(50.0)

        assertEquals(0, waitingLine.size, "expected empty queue")
    }
}
