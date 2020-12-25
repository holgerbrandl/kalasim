package org.kalasim.scratch

import org.kalasim.*

class ResourceDocu {

    fun main() {
        val clerks = Resource("clerks", capacity = 3)
        val assistence = Resource("assistence", capacity = 3)

        Component().request(clerks withQuantity 2)

        Component().request(clerks withQuantity 1, assistence withQuantity 2)


// release
        val r = Resource("clerks", capacity = 3)
        r.release(2.0)
        Component().release(r)


    }
}


object ProcessGraph {

    class Customer(val clerk: Resource) : Component() {

        override fun process() = sequence {
            // do shopping
            yield(hold(duration = 23.0))

            // wait for an empty counter
            request(clerk)

            // billing process
            yield(hold(duration = 2.0, priority = 3))
        }
    }
}

object EventLog {

    @JvmStatic
    fun main(args: Array<String>) {
        // create simulation with no default logging
        val sim = createSimulation { }

        // add custom log consumer
        sim.addTraceListener(TraceListener { traceElement -> TODO("do something with") })
    }
}