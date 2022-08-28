#!/bin/env kscript


//DEPS com.github.holgerbrandl:kalasim:0.6.91
//DEPS com.github.holgerbrandl:krangl:0.17

///@file:DependsOn("com.github.holgerbrandl:kalasim:0.6.91")
//@file:DependsOn("com.github.holgerbrandl:krangl:0.17")

import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType
import org.kalasim.misc.repeat
import java.io.File


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
    enableEventLog()

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
val requestsDf = requests.asDataFrame()
    .groupBy("requester")
    .sortBy("timestamp")
    .add("end_time") { prev()?.get("timestamp") as TickTime }
    .add("state") { if(index().rem(2) == 0) "hungry" else "eating" }
    .filter { "quantity"<Int>() > 0 }
    .concat()

println("writing sim-csv")
requestsDf.writeCSV(File("dining.csv"))
