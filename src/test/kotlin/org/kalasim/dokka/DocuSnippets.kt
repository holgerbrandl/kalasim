@file:Suppress("unused", "OPT_IN_USAGE") // seem a dokka bug

package org.kalasim.dokka

import org.kalasim.*
import org.kalasim.Priority.Companion.IMPORTANT
import kotlin.time.Duration.Companion.minutes


fun statesHowTo() {
    createSimulation {
        val trafficLight: State<String> = State("red")

        object : Component("Car") {
            override fun process() = sequence {
                hold(14.minutes, "driving")
                wait(trafficLight, "green")
                hold(7.minutes, "driving again")

                // or using predicate syntax
                wait(trafficLight) { it.startsWith("gr") }
            }
        }

        object : Component("TrafficController") {
            override fun repeatedProcess() = sequence {
                hold(3.minutes)
                trafficLight.value = "green"
                hold(3.minutes)
                trafficLight.value = "red"

                // alternatively we could also use a state trigger
                trafficLight.trigger("green", max = 1)
            }
        }

        run()
    }
}

fun resourceHowTo() {
    val clerks = Resource("clerks", capacity = 3)
    val assistance = Resource("assistence", capacity = 3)

    object : Component() {
        override fun process() =
            sequence {
                // the recommended way is to use request that is released automatically
                request(clerks) {
                    // consume it
                    hold(2.minutes)
                }
                // no need to release it, if a `honorBlock` is provided it will be released automatically

                // api to request resources with custom quantity and priorities
                request(clerks, quantity = 2, priority = IMPORTANT)

                // it also provides a more streamlined dsl support
                request(clerks withQuantity 2)
                request(clerks withQuantity 1, assistance withQuantity 2)
                request(clerks withPriority 1)
                request(clerks withPriority Priority(2))
                request(clerks withQuantity 3.4 andPriority Priority(1))
                request(clerks withQuantity 3.4 andPriority 3)


                hold(2.minutes) // consume it

                // note the requests from above are accumulated and need to be released manually
                release(clerks, quantity = 2.0) // release some quantity

                release(clerks) // release entire claim
            }
    }
}


fun eventsHowTo() {
    class MyEvent(val msg: String, time: TickTime) : Event(time)

    // create simulation with no default logging
    val sim = createSimulation {
        // in place event processing
        addEventListener { println(it) }

        // we could also enable a global event log
        enableEventLog()

        object : Component() {
            override fun process() = sequence<Component> {
                log(MyEvent("something magical happened", now))
            }
        }
    }

    // register a custom log consumer for MyEvent
    sim.addEventListener<MyEvent> { event -> println("oh my: ${event.msg}") }

    sim.run()

    // or query them from the global event log
    sim.get<EventLog>().filterIsInstance<MyEvent>().forEach { println(it.msg) }
}



