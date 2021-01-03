package org.kalasim.scratch

import org.kalasim.*

class ResourceDocu {

    fun main() {

        val clerks = Resource("clerks", capacity = 3)
        val assistance = Resource("assistence", capacity = 3)

        object : Component(){
            override fun process() = sequence<Component> {
                request(clerks withQuantity 2)
                request(clerks withQuantity 1, assistance withQuantity 2)
                request(clerks withPriority  1)
                request(clerks withQuantity 3.4 andPriority  1 )

                hold(2) // consume it

                release(clerks, quantity= 2.0) // release some quantity

                release(clerks) // release entiry claim
            }
        }

        // we can also release all claims from the resource itself
        val r = Resource("clerks", capacity = 3)
        r.release(2.0)

    }
}


object ProcessGraph {

    class Customer(val clerk: Resource) : Component() {

        override fun process() = sequence {
            // do shopping
            hold(duration = 23.0)

            // wait for an empty counter
            request(clerk)

            // billing process
            hold(duration = 2.0, priority = 3)
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

object Hold1 {
    @JvmStatic
    fun main(args: Array<String>) {

        object : Component() {
            override fun process() = sequence<Component>{
                hold(5.0)
                hold(5.0)
            }
        }

        object : Component() {
            override fun process() = sequence {
                holdMinutes()
                holdMinutes()
            }

            private suspend fun SequenceScope<Component>.holdMinutes() {
                hold(5.0)
            }
        }

    }
}