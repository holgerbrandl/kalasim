package org.kalasim.examples.er

import kravis.geomBar
import kravis.nshelper.plot
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.letsPlot.geom.geomBar
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


            val df = dataFrameOf(columnOf(*arrivals.toTypedArray()).named("patient"))
                .add("type") { "patient"<Patient>().type }
                .add("status") { "patient"<Patient>().patientStatus.value }
                .add("severity") { "patient"<Patient>().severity }

            df.groupBy("status").count().print()
            df.rowsCount()

           // https://github.com/JetBrains/lets-plot-kotlin/issues/82
//            (df.letsPlot{ x = "status" } + geomBar ()).show()
            df.asKranglDF().plot(x="status").geomBar().show()
            Thread.sleep(101000)

            // visualize room setup as gant chart
        }
    }
}