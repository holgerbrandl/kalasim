package org.kalasim.examples.hospital

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import krangl.DataFrameRow
import krangl.count
import krangl.dataFrameOf
import krangl.print
import org.kalasim.*
import org.kalasim.examples.MM1Queue
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.examples.er.Patient
import org.kalasim.plot.letsplot.display
import org.koin.core.Koin
import org.koin.core.logger.EmptyLogger
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }

    // run for a week
//        run(24 * 14)

    val kryo = Kryo()
    kryo.register(EmergencyRoom::class.java)
    kryo.register(Koin::class.java)
    kryo.register(EmptyLogger::class.java)
    kryo.register(org.koin.core.logger.Level::class.java)
    kryo.register(java.util.HashSet::class.java)
    kryo.register(org.koin.core.module.Module::class.java)


    val output = Output(FileOutputStream("file.bin"))
    kryo.writeObject(output, sim)

    // analysis
    sim.run(10)
//    sim.testSim()
}

public fun EmergencyRoom.testSim() = apply {
    incomingMonitor.display("Incoming Patients")
    treatedMonitor.display("Treated Patients")
    deceasedMonitor.display("Deceased Patients")

    get<EmergencyRoom>().apply {
        rooms[0].setup.timeline.display().show()
        rooms[1].setup.timeline.display().show()

        rooms[1].statusTimeline.display().show()
    }

    waitingLine.sizeTimeline.display().show()
//    waitingLine.timlengthOfStayMonitor.display().show()

    val arrivals = get<ComponentGenerator<Patient>>().history
//        arrivals.asDataFrame().print()


    val df = arrivals.map {
        mapOf(
            "type" to it.type.toString(),
            "status" to it.patientStatus.value.toString(),
            "severity" to it.severity.value.toString()
        ) as DataFrameRow
    }.let { dataFrameOf(it) }

    df.count("status").print()

}

//https://gist.github.com/sgdan/eaada2f243a48196c5d4e49a277e3880
fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
