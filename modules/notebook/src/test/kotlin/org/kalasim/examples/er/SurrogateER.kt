package org.kalasim.examples.er

import kravis.device.SwingPlottingDevice
import kravis.device.showFile
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.analysis.InteractionEvent
import org.kalasim.analysis.ResourceActivityEvent
import org.kalasim.collect
import org.kalasim.enableEventLog
import org.kalasim.exponential
import org.kalasim.misc.QuartoUtil
import org.kalasim.plot.kravis.displayStateCounts
import org.kalasim.plot.kravis.displayStayDistributions
import org.kalasim.plot.kravis.displayTimelines
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

object BigER {
    @JvmStatic
    fun main(args: Array<String>) {
        val outputDir = Path("modules/notebook/src/test/kotlin/org/kalasim/examples/er")

        val er = EmergencyRoom(
            numPhysicians = 50,
            numRooms = 30,
            patientArrival = exponential(0.05.hours),
            enableInternalMetrics = false
        )
        er.apply {

            val rae = collect<ResourceActivityEvent>()
            val interactions = collect<InteractionEvent>()
            run(365.days)
//            SessionPrefs.RENDER_BACKEND = LocalR()

//            println(er.waitingLine.statistics)
////            er.waitingLine.sizeTimeline.display().show()
//
//            er.waitingLine.lengthOfStayStatistics.display().show()
//            er.waitingLine.sizeTimeline.display().show()

            if (true) {
//            rae.toDataFrame().plot(xmin to 0.0)
                rae.toDataFrame().writeCSV(outputDir.resolve("er.bookings.csv").toFile())
                interactions.toDataFrame().writeCSV(outputDir.resolve("er.interactions.csv").toFile())

                QuartoUtil.runQuartoInDir(outputDir, "er_analysis.R")
            }
        }

    }
}

object ProfileER {
    @JvmStatic
    fun main(args: Array<String>) {
        val er = EmergencyRoom(
            numPhysicians = 5,
            numRooms = 10,
            patientArrival = exponential(0.05.hours),
            enableInternalMetrics = false
        )
        er.apply {
            run(10000.days)
        }
    }
}


object ErDisplayTests {
    @JvmStatic
    fun main(args: Array<String>) {
        val er = EmergencyRoom(
//            numPhysicians = 5,
//            numRooms = 10,
//            patientArrival = exponential(0.05.hours),
        )
        er.enableEventLog()

        val rae = er.collect<ResourceActivityEvent>()
        er.apply {
            run(20.days)
        }

        kravis.SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()

        er.doctors.displayTimelines().showFile()
//        er.doctors.displayTimeline(byRequester = true, colorBy = { it.requester.name}).showFile()
//        rae.display().show()

//        er.doctors.foo2()

        er.patients.map { it.patientStatus }.displayStateCounts().showFile()

        er.patients.take(10).map { it.patientStatus }.displayTimelines(to = er.startDate + 4.hours).showFile()
        er.patients.drop(100).take(10).map { it.patientStatus }.let {
            val minBy = it.minOf { it.timeline.statsData().asList().first().timestamp }
            it.displayTimelines(from = minBy, to = minBy + 100.hours, title = "100-110").showFile()
        }

        er.patients.map{it.patientStatus}.displayStayDistributions().showFile()
//        er.patients.map{it.patientStatus.timeline}.displayTimelines().showFile()


        Thread.sleep(6000)
    }
}




