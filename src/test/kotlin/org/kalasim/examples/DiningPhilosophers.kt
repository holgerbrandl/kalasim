//DiningPhilosophers.kt
package org.kalasim.examples

import krangl.*
import kravis.geomSegment
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.*
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType
import org.kalasim.misc.repeat


fun main() {
    class Fork : Resource()

    class Philosopher(name: String, val leftFork: Fork, val rightFork: Fork) : Component(name) {
        val thinking = exponential(1)
        val eating = exponential(1)

        override fun process() = sequence {
            while (true) {
                hold(thinking())
                request(leftFork) {
                    hold(0.1) // wait before taking the second fork
                    request(rightFork) {
                        hold(eating())
                        log("$name is eating")
                    }
                }
            }
        }
    }

    val sim = createSimulation(true) {
        val ec = collect<Event>()

        // create forks and resources
        val names = listOf("Socrates", "Pythagoras", "Plato", "Aristotle")
        val forks = repeat(names.size) { Fork() } //.repeat().take(names.size + 1).toList()

        names.forEachIndexed { idx, name ->
            Philosopher(name, forks[idx], forks[(idx + 1).rem(forks.size)])
        }

        run(50)
    }

    // Analysis (gather monitoring data (as in simmer:get_mon_arrivals)
    data class RequestRecord(val requester: String, val timestamp: TickTime, val resource: String, val quantity: Double)

    val tc = sim.get<EventLog>()
    val requests = tc.filterIsInstance<ResourceEvent>().map {
        val amountDirected = (if (it.type == ResourceEventType.RELEASED) -1 else 1) * it.amount
        RequestRecord(it.requester.name, it.time, it.resource.name, amountDirected)
    }

    // transform data into shape suitable for interval plotting
    val requestsDf = requests.toDataFrame()

        .groupBy("requester")
        .sortBy("requester", "timestamp")
        // TODO how to express lag here?
//        .add("end_time") {   it["timestamp"].lag() }
        .add("end_time") {   next()?.get("timestamp") }
        .add("state") { if (index().rem(2) == 0) "hungry" else "eating" }
        .filter { "quantity"<Int>() > 0 }
//        .ungroup()

    // visualize with kravis
    // todo bring back once kravis has been fully ported
//    requestsDf
//        .toKranglDF()
//        .plot(x = "timestamp", xend = "end_time", y = "requester", yend = "requester", color = "state")
//        .geomSegment(size = 15.0)
}