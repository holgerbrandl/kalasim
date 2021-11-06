package org.kalasim.examples.hospital

import EmergencyRoom
import Patient
import SetupAvoidanceNurse
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import krangl.DataFrameRow
import krangl.count
import krangl.dataFrameOf
import krangl.print
import org.kalasim.*
import org.kalasim.demo.MM1Queue
import org.kalasim.plot.letsplot.display
import org.koin.core.Koin
import org.koin.core.component.get
import org.koin.core.definition.BeanDefinition
import org.koin.core.definition.Callbacks
import org.koin.core.instance.SingleInstanceFactory
import org.koin.core.logger.EmptyLogger
import org.koin.core.registry.InstanceRegistry
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }

    // run for a week
//        run(24 * 14)

    val kryo = Kryo()

    kryo.setReferences(true)
    kryo.register(EmergencyRoom::class.java)
    kryo.register(Koin::class.java)
    kryo.register(EmptyLogger::class.java)
    kryo.register(org.koin.core.logger.Level::class.java)
    kryo.register(java.util.HashSet::class.java)
    kryo.register(org.koin.core.module.Module::class.java)
    kryo.register(MM1Queue::class.java)
    kryo.register(InstanceRegistry::class.java)
    kryo.register(BeanDefinition::class.java)
    kryo.register(SingleInstanceFactory::class.java)
    kryo.register(Callbacks::class.java)
    kryo.register(ConcurrentHashMap::class.java)


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
        rooms[0].setup.valueMonitor.display().show()
        rooms[1].setup.valueMonitor.display().show()

        rooms[1].statusMonitor.display().show()
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

}

//https://gist.github.com/sgdan/eaada2f243a48196c5d4e49a277e3880
fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
