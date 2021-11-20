package org.kalasim.scratch

import org.kalasim.*

class ResourceDocu {

    fun main() {

        val clerks = Resource("clerks", capacity = 3)
        val assistance = Resource("assistence", capacity = 3)

        object : Component() {
            override fun process() = sequence<Component> {
                // the recommended way is to use request that is released automatically
                request(clerks) {
                    // consume it
                    hold(2)
                }
                // no need to release it, if a `honorBlock` is provided it will be released automatically


                // api to request resources with custom quantity and priorities
                request(clerks withQuantity 2)
                request(clerks withQuantity 1, assistance withQuantity 2)
                request(clerks withPriority 1)
                request(clerks withQuantity 3.4 andPriority Priority(1))

                hold(2) // consume it

                release(clerks, quantity = 2.0) // release some quantity

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
            hold(ticks = 23.0)

            // wait for an empty counter
            request(clerk) {

                // billing process
                hold(ticks = 2.0, priority = IMPORTANT)
            }
        }
    }
}

object EventLog {

    @JvmStatic
    fun main(args: Array<String>) {
        // create simulation with no default logging
        val sim = createSimulation {
            addEventListener{ it: Event -> println(it)}

            class MyEvent(msg:String, time:TickTime ) : Event(time)

            object: Component(){
                override fun process() = sequence<Component> {
                    log(MyEvent("something magical happened", now))
                }
            }
        }

        // add custom log consumer
        sim.addEventListener(EventListener { traceElement -> TODO("do something with") })
    }
}

object Hold1 {
    @JvmStatic
    fun main(args: Array<String>) {

        object : Component() {
            override fun process() = sequence<Component> {
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