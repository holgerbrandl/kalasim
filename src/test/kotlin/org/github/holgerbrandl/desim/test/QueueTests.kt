package org.github.holgerbrandl.desim.test

import org.github.holgerbrandl.desim.Component
import org.github.holgerbrandl.desim.ComponentQueue
import org.github.holgerbrandl.desim.add
import org.github.holgerbrandl.desim.examples.koiner.createSimulation
import org.junit.Test
import kotlin.test.assertEquals

class QueueTests {

    @Test
    fun testWaitingLine() {

        class Foo : Component()

        val tc = TraceCollector()
        val waitingLine by lazy { ComponentQueue<Foo>() }

        val env = createSimulation {
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
