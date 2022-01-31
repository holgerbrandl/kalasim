package org.kalasim.examples.hospital

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.security.AnyTypePermission
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
import org.koin.core.registry.InstanceRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }


    //save simulation

    // save it to xml
    //    https://github.com/x-stream/xstream/issues/101
//    val xstream = XStream()
    val xstream = XStream(DomDriver())
//    val xstream = XStream(JsonHierarchicalStreamDriver()) //--> does not work because embeed json driver can just WRITE!
    xstream.setMode(XStream.XPATH_RELATIVE_REFERENCES);

    val envXML = xstream.toXML(sim)
    File("test.xml").writeBytes(gzip(envXML))

//    val xstream = XStream(JsonHierarchicalStreamDriver())--> does not work because embeed json driver can just WRITE!
//    xstream.setMode(XStream.NO_REFERENCES)

//    env.getKoin()
    println(xstream.toXML(envXML))
    xstream.addPermission(AnyTypePermission.ANY); // to prevent https://stackoverflow.com/questions/30812293/com-thoughtworks-xstream-security-forbiddenclassexception
    val restoredEnv: Environment = xstream.fromXML(envXML) as Environment

    println("running restored simulation")
    restoredEnv.run(10.0)
}