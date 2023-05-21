package org.kalasim.examples.er

import com.github.holgerbrandl.kdfutils.toKranglDF
import kravis.geomBar
import kravis.plot
import org.jetbrains.kotlinx.dataframe.api.*
import org.kalasim.ComponentGenerator
import org.kalasim.plot.kravis.display
import kotlin.time.Duration.Companion.days


object SimpleER {
    @JvmStatic
    fun main(args: Array<String>) {
        EmergencyRoom(nurse = RefittingAvoidanceNurse).apply {

            run(7.days)

            // analysis
            incomingMonitor.display("Incoming Patients")
            treatedMonitor.display("Treated Patients")
            deceasedMonitor.display("Deceased Patients")

            get<EmergencyRoom>().apply {
                rooms[0].setup.timeline.display().show()
                rooms[1].setup.timeline.display().show()

                rooms[1].stateTimeline.display().show()
            }

            waitingLine.sizeTimeline.display().show()
            waitingLine.lengthOfStayStatistics.display().show()

            val arrivals = get<ComponentGenerator<Patient>>().history
//        history.toDataFrame().print()


            val df = dataFrameOf(columnOf(*arrivals.toTypedArray()).named("patient"))
                .add("type") { "patient"<Patient>().type }
                .add("status") { "patient"<Patient>().patientStatus.value }
                .add("severity") { "patient"<Patient>().severity }

            df.groupBy("status").count().print()
            df.rowsCount()

           // https://github.com/JetBrains/lets-plot-kotlin/issues/82
//            (df.letsPlot{ x = "status" } + geomBar ()).show()
            df.plot(x="status").geomBar().show()
            Thread.sleep(101000)

            // visualize room setup as gant chart
        }
    }
}