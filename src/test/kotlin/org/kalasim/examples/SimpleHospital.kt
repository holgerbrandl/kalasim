package org.kalasim.examples

import krangl.*
import kravis.*
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.plot.kravis.display

fun main() {
    val sim = createSimulation(true) {
        // actually it would be more efficient to simply model a single Doctor with a capacity of 2
        val nurses = Resource("nurses", 1)
        val doctors = Resource("doctors", 2)
        val admin = Resource("admin", 1)

        enableEventLog()

        class Patient : Component() {
            override fun process() = sequence {
                // add an intake activity
                request(nurses) {
                    hold(normal(15).sample())
                }

                // add a consultation activity
                request(doctors) {
                    hold(normal(20).sample())
                }

                // add a planning activity
                request(admin) {
                    hold(normal(5).sample())
                }
            }
        }

        ComponentGenerator(normal(10, 2)) {
            Patient()
        }


        run(100)

        nurses.occupancyTimeline.display()
        nurses.capacityTimeline.display()
    }

    // analysis
    val tc = sim.get<EventLog>()
    val requests: List<ResourceEvent> = tc.filterIsInstance<ResourceEvent>()
    println(requests.first())

    val requestDF = requests.asDataFrame()
    requestDF.print()


    // see org/kalasim/examples/DiningPhilosophers.kt:57
    val records = requests
        .groupBy { it.requester }
        .mapValues { kv -> kv.value.sortedWith(compareBy({ it.requester.name }, { it.time })) }
        .values.flatten()
//    println(records)
    records.forEach { println("$it") }


    records.displayTimeline()

//    sim.components
    // component state heatmap

}

fun List<ResourceEvent>.displayTimeline(
    resources: List<Resource>? = null,
    items: List<String> = listOf("capacity", "requesters", "claimers"),
    avg: Boolean = true
): GGPlot {
    // see https://github.com/r-simmer/simmer.plot/blob/master/R/plot.resources.R

    val data = this.filter { resources == null || resources.contains(it.resource) }

    val env = data.first().requester.env

//    return data.plot(
//        x = ResourceEvent::eventTime,
//        y = ResourceEvent::occupancy
//    ).xLabel("time").yLabel("").geomStep()

    var df = data.asDataFrame()
//        .remove("type", "requester", "occupancy", "amount")
        .select("time", "resource", "capacity", "claimed", "requesters")
        .gather("statistic", "value", { listOf("capacity", "claimed", "requesters") })
//        .sortedBy("resource", "time")

    // calculate smooth aggregate
    df = df.groupBy("resource", "value")
        .addColumn("mean") { (it["value"] * (it["time"].lead() - it["time"])).cumSum() / it["time"] }.ungroup()


    // TODO replicate end of each resource until end of simulation

    df.head(20).print()
    // complement step fun until now


    return df.plot(x = "time", y = if (avg) "mean" else "value", color = "statistic")
        .xLabel("time").yLabel("").geomStep(alpha = 0.6).facetWrap("resource")
}

@JvmName("displayString")
fun List<String>.display() {


}
//fun EventLog.get