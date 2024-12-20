//DiningPhilosophers.kt
package org.kalasim.examples

import kravis.geomSegment
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.*
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType
import org.kalasim.misc.repeat
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


fun main() {
    class Fork : Resource()

    class Philosopher(name: String, val leftFork: Fork, val rightFork: Fork) : Component(name) {
        val thinking = exponential(1.minutes)
        val eating = exponential(1.minutes)

        override fun process() = sequence {
            while(true) {
                hold(thinking())
                request(leftFork) {
                    hold(10.seconds) // wait before taking the second fork

                    request(rightFork) {
                        hold(eating())
                        log("$name is eating")
                    }
                }
            }
        }
    }

    val sim = createSimulation {
        enableComponentLogger()

//        val ec = collect<Event>()

        // create forks and resources
        val names = listOf("Socrates", "Pythagoras", "Plato", "Aristotle")
        val forks = repeat(names.size) { Fork() } //.repeat().take(names.size + 1).toList()

        names.forEachIndexed { idx, name ->
            Philosopher(name, forks[idx], forks[(idx + 1).rem(forks.size)])
        }

        run(50.minutes)
    }

    // Analysis (gather monitoring data (as in simmer:get_mon_arrivals)
    data class RequestRecord(val requester: String, val timestamp: SimTime, val resource: String, val quantity: Double)

    val tc = sim.get<EventLog>()

    val requests = tc.filterIsInstance<ResourceEvent>().map {
        val amountDirected = (if(it.type == ResourceEventType.RELEASED) -1 else 1) * it.amount
        RequestRecord(it.requester.name, it.time, it.resource.name, amountDirected)
    }

    // transform data into shape suitable for interval plotting
    val requestsDf = requests.toDataFrame()
        .groupBy("requester")
        .sortBy("requester", "timestamp")
//        .add("end_time") {   it["timestamp"].lag() }
        .add("end_time") { next()?.get("timestamp") }
        .add("state") { if(index().rem(2) == 0) "hungry" else "eating" }
        .filter { "quantity"<Int>() > 0 }
        .toDataFrame()

    // visualize with kravis
    requestsDf
        .plot(x = "timestamp", xend = "end_time", y = "requester", yend = "requester", color = "state")
        .geomSegment(size = 15.0)
}