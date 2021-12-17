//DiningPhilosophers.kt
package org.kalasim.examples

import krangl.asDataFrame
import krangl.gt
import krangl.lag
import kravis.geomSegment
import kravis.plot
import org.kalasim.*
import org.kalasim.misc.repeat


fun main() {
    class Fork : Resource()

    class Philosopher(name: String, val leftFork: Fork, val rightFork: Fork) : Component(name) {
        val thinking = exponential(1)
        val eating = exponential(1)

        override fun process() = sequence {
            while(true) {
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


    class DiningTable : Environment() {

        // create forks and resources
        val names = listOf("Socrates", "Pythagoras", "Plato", "Aristotle")
        val forks = repeat(names.size) { Fork() } //.repeat().take(names.size + 1).toList()

        init {
            names.forEachIndexed { idx, name ->
                Philosopher(name, forks[idx], forks[(idx + 1).rem(forks.size)])
            }

            eventLog()
        }
    }

    val sim = DiningTable()
    sim.run(50)

    // Analysis (gather monitoring data (as in simmer:get_mon_arrivals)
    data class RequestRecord(val requester: String, val timestamp: TickTime, val resource: String, val quantity: Double)

    val tc = sim.get<EventLog>()
    val requests = tc.filterIsInstance<ResourceEvent>().map {
        val amountDirected = (if(it.type == ResourceEventType.RELEASED) -1 else 1) * it.amount
        RequestRecord(it.requester.name, it.time, it.resource.name, amountDirected)
    }

    // transform data into shape suiteable for interval plotting
    val requestsDf = requests.asDataFrame()
        .groupBy("requester")
        .sortedBy("requester", "timestamp")
        .addColumn("end_time") { it["timestamp"].lag() }
        .addColumn("state") { rowNumber.map { if(it.rem(2) == 0) "hungry" else "eating" } }
        .filter { it["quantity"] gt 0 }
        .ungroup()

    // visualize with kravis
    requestsDf.plot(x = "timestamp", xend = "end_time", y = "requester", yend = "requester", color = "state")
        .geomSegment(size = 15.0).show()
}