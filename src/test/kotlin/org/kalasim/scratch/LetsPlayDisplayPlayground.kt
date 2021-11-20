package org.kalasim.test

import org.kalasim.OffsetTransform
import org.kalasim.demo.MM1Queue
import org.kalasim.plot.letsplot.display
import java.time.Instant
import java.util.concurrent.TimeUnit

fun main() {
    val mm1 = MM1Queue()

    // redo but with set tick-transform
    mm1.tickTransform = OffsetTransform(
        offset = Instant.parse("2021-01-01T00:00:00.00Z"),
        tickUnit = TimeUnit.MINUTES
    )

    mm1.run(50)

//    mm1.server.activities.display("MM1 Server Utilization").show()

//    mm1.componentGenerator.arrivals.displayStateTimeline("MM1 Server Utilization").show()


//    val customerTimeline =
//        mm1.componentGenerator.arrivals.first().statusTimeline
//
//    customerTimeline.display("Arrival State Timeline").show()

    mm1.server.claimedTimeline.display("Claimed Server Capacity").show()
}