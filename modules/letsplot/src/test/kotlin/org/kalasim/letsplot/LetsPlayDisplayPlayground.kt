package org.kalasim.test

import org.kalasim.examples.MM1Queue
import org.kalasim.plot.letsplot.display
import org.kalasim.plot.letsplot.displayStateTimeline
import kotlin.time.DurationUnit
import kotlin.time.measureTime

fun main() {
    val maxElements = 2000000
    val mm1 = MM1Queue(0.25, mu=3.0)

    val time = measureTime {

        var numGenerated = 0
        mm1.componentGenerator.addConsumer { if (numGenerated++ > maxElements) mm1.stopSimulation() }


        mm1.run()
    }

    println("runtime for ${maxElements}: ${maxElements.toDouble() / time.toDouble(DurationUnit.SECONDS)} events/second")

    mm1.server.activities.display("MM1 Server Utilization").show()

    mm1.componentGenerator.history.displayStateTimeline("MM1 Server Utilization").show()

    val customerTimeline =
        mm1.componentGenerator.history.first().stateTimeline

    customerTimeline.display("Arrival State Timeline").show()

//    mm1.server.claimedTimeline.display("Claimed Server Capacity").show()
    mm1.server.requesters.queueLengthTimeline.display("Queue Length").show()
    println("mean queue length is ${mm1.server.requesters.statistics.lengthStats}")
}