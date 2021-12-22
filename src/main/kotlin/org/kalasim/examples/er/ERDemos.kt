package org.kalasim.examples.er

import krangl.count
import krangl.dataFrameOf
import krangl.print
import org.kalasim.ComponentGenerator
import org.kalasim.plot.kravis.display

object InfiniteER {
    @JvmStatic
    fun main(args: Array<String>) {
        val er = EmergencyRoom(disableMetrics = true)

        er.run(100000)

        // memory analysis
        println()
    }
}

object SimpleER {
    @JvmStatic
    fun main(args: Array<String>) {
        EmergencyRoom(RefittingAvoidanceNurse).apply {

            // run for a week
            run(24 * 14)

            // analysis
            incomingMonitor.display("Incoming Patients")
            treatedMonitor.display("Treated Patients")
            deceasedMonitor.display("Deceased Patients")

            get<EmergencyRoom>().apply {
                rooms[0].setup.timeline.display().show()
                rooms[1].setup.timeline.display().show()

                rooms[1].statusTimeline.display().show()
            }

            waitingLine.queueLengthTimeline.display().show()
            waitingLine.lengthOfStayTimeline.display().show()

            val arrivals = get<ComponentGenerator<Patient>>().history
//        history.asDataFrame().print()


            val df = arrivals.map {
                mapOf(
                    "type" to it.type.toString(),
                    "status" to it.patientStatus.value.toString(),
                    "severity" to it.severity.value.toString()
                )
            }.let { dataFrameOf(it) }

            df.count("status").print()

            // visualize room setup as gant chart
        }
    }
}