package org.kalasim.examples.er

import org.jetbrains.kotlinx.dataframe.api.*
import org.kalasim.ComponentGenerator
import org.kalasim.plot.kravis.display

object InfiniteER {
    @JvmStatic
    fun main(args: Array<String>) {
        val er = EmergencyRoom(enableTickMetrics = true)

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

            waitingLine.sizeTimeline.display().show()
            waitingLine.lengthOfStayStatistics.display().show()

            val arrivals = get<ComponentGenerator<Patient>>().history
//        history.asDataFrame().print()


            val df = dataFrameOf(columnOf(arrivals).named("patient"))
                .add("type"){ "patient"<Patient>().type.toString()}
                .add("status"){ "patient"<Patient>().patientStatus.toString()}
                .add("severity"){ "patient"<Patient>().severity.toString()}

            df.groupBy("status").count().print()
            df.rowsCount()

            // visualize room setup as gant chart
        }
    }
}