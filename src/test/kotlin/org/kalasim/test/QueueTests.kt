package org.kalasim.test

import org.kalasim.Component
import org.kalasim.ComponentQueue
import org.kalasim.add
import org.kalasim.configureEnvironment
import org.junit.Test
import kotlin.test.assertEquals

class QueueTests {

    @Test
    fun testWaitingLine() {

        class Foo : Component()

        val tc = TraceCollector()
        val waitingLine by lazy { ComponentQueue<Foo>() }

        val env = configureEnvironment {
            add { waitingLine }
        }

        env.addTraceListener(tc)

        waitingLine.add(Foo())
        waitingLine.add(Foo())
        waitingLine.add(Foo())

        // consume it
        assertEquals(4, waitingLine.size, "expected 3 items in queue")


        // add a consumer
        env.addComponent(object : Component() {
            override suspend fun SequenceScope<Component>.process(it: Component) {
                while (waitingLine.isNotEmpty()) {
                    yield(waitingLine.poll())
                    yield(hold(5.0))
                }

                yield(it.passivate())
            }
        })

        env.run(50.0)

        assertEquals(0, waitingLine.size, "expected empty queue")
    }
}
