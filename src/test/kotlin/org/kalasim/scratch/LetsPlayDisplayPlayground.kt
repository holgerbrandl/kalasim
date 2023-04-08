package org.kalasim.test

import org.kalasim.examples.MM1Queue
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.plot.letsplot.display

@OptIn(AmbiguousDuration::class)
fun main() {
    val mm1 = MM1Queue()

    // redo but with set tick-transform
//    mm1.startDate = Instant.parse("2021-01-01T00:00:00.00Z")
//    mm1.startTime = OffsetTransform(
//        offset = Instant.parse("2021-01-01T00:00:00.00Z"),
//        tickUnit = DurationUnit.MINUTES
//    )

    mm1.run(50)

//    mm1.server.activities.display("MM1 Server Utilization").show()

//    mm1.componentGenerator.history.displayStateTimeline("MM1 Server Utilization").show()


//    val customerTimeline =
//        mm1.componentGenerator.history.first().statusTimeline
//
//    customerTimeline.display("Arrival State Timeline").show()

    mm1.server.claimedTimeline.display("Claimed Server Capacity").show()
}