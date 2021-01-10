//DiningPhilosophers.kt
package org.kalasim.examples

import krangl.*
import org.kalasim.*
import org.kalasim.misc.repeat
import org.koin.core.component.get


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

    val sim = createSimulation(true) {
        traceCollector()

        // create forks and resources
        val names = listOf("Socrates", "Pythagoras", "Plato", "Aristotle")
        val forks = repeat(names.size) { Fork() }.repeat().take(names.size + 1).toList()
        names.forEachIndexed { idx, name ->
            Philosopher(name, forks[idx], forks[(idx + 1).rem(forks.size)])
        }

        run(100)
    }

    // Analysis (gather monitoring data (as in simmer:get_mon_arrivals)
    data class RequestRecord(val requester: String, val timestamp: Double, val resource: String, val quantity: Double)

    val tc = sim.get<TraceCollector>()
    val requests = tc.filterIsInstance<ResourceEvent>().map {
        val amountDirected = (if(it.type == ResourceEventType.RELEASED) -1 else 1) * it.amount
        RequestRecord(it.requester.name, it.time, it.resource.name, amountDirected)
    }

    val requestsDf = requests.asDataFrame()
        .groupBy("requester")
        .sortedBy("requester", "timestamp")
        .addColumn("end_time") { it["timestamp"].lag() }
        .filter { it["quantity"] gt 0 }

    // reshape into form suitable for plotting
    requestsDf.schema()
//        requestsDf.plot().geomSegment()

    requestsDf.print()
//        requestsDf.groupBy("requester").sortedBy("time").addColumns(
//            "end_time" to { it["time"].lag() }
//        )

//        requestsDf.plot(x = "requester", x=)
}