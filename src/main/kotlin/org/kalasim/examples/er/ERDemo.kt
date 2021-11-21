package org.kalasim.examples.er

import EmergencyRoom
import Patient
import RefittingAvoidanceNurse
import krangl.*
import org.kalasim.ComponentGenerator
import org.kalasim.plot.kravis.display
import kotlin.to


fun main() {
    val sim = EmergencyRoom(RefittingAvoidanceNurse).apply {

        // run for a week
        run(24 * 14)

        // analysis
        incomingMonitor.display("Incoming Patients")
        treatedMonitor.display("Treated Patients")
        deceasedMonitor.display("Deceased Patients")

        get<EmergencyRoom>().apply {
            rooms[0].setup.valueMonitor.display().show()
            rooms[1].setup.valueMonitor.display().show()

            rooms[1].statusTimeline.display().show()
        }

        waitingLine.queueLengthMonitor.display().show()
        waitingLine.lengthOfStayMonitor.display().show()

        val arrivals = get<ComponentGenerator<Patient>>().arrivals
//        arrivals.asDataFrame().print()


        val df = arrivals.map {
            mapOf(
                "type" to it.type.toString(),
                "status" to it.patientStatus.value.toString(),
                "severity" to it.severity.value.toString()
            ) as DataFrameRow
        }.let { dataFrameOf(it) }

        df.count("status").print()

        // visualize room setup as gant chart
    }
}