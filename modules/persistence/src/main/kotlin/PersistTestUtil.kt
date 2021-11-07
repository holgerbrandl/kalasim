import krangl.DataFrameRow
import krangl.count
import krangl.dataFrameOf
import krangl.print
import org.kalasim.ComponentGenerator
import org.kalasim.plot.letsplot.display
import org.koin.core.component.get
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


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
    GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun ungzip(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
