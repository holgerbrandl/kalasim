package org.kalasim.examples.hospital

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import krangl.DataFrameRow
import krangl.count
import krangl.dataFrameOf
import krangl.print
import org.kalasim.*
import org.kalasim.examples.MM1Queue
import org.kalasim.plot.letsplot.display
import org.koin.core.Koin
import org.koin.core.component.get
import org.koin.core.logger.EmptyLogger
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@OptIn(ExperimentalStdlibApi::class)
fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }



        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
//            .add(SimProcess::class.java, JsonAdapter<SimProcess>{})
            .build()

//        val adapter = moshi.adapter<Environment>()
//        val toJson = adapter.toJson(this)



    // analysis
//    sim.testSim()
//    restoredSim.run(10)

    // visualize room setup as gant chart

}
