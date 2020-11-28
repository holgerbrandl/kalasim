package org.kalasim.misc

import com.google.gson.Gson
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver
import org.kalasim.Component
import org.kalasim.Environment
import org.kalasim.Resource
import org.kalasim.createSimulation

object XStreamPersistence {
    @JvmStatic
    fun main(args: Array<String>) {
        class Foo() : Component()

        val env = createSimulation {
            Foo()
            Resource()
        }

        // save it to xml
        //    https://github.com/x-stream/xstream/issues/101
        val xstream = XStream(JsonHierarchicalStreamDriver())
//    val xstream = XStream(JsonHierarchicalStreamDriver())--> does not work because embeed json driver can just WRITE!
//    xstream.setMode(XStream.NO_REFERENCES)

        val envXML = xstream.toXML(env)

//    env.getKoin()
        println(xstream.toXML(env))
        val restoredEnv: Environment = xstream.fromXML(envXML) as Environment

        println("running restored simulation")
        restoredEnv.run(10.0)
    }
}

// does not work yet
object GsonPersistence {
    @JvmStatic
    fun main(args: Array<String>) {
        class Foo : Component()

        val env = createSimulation {
            Foo()
            Resource()
        }

        // save it to xml
        //    https://github.com/x-stream/xstream/issues/101
        val gson = Gson()
//    val xstream = XStream(JsonHierarchicalStreamDriver())--> does not work because embeed json driver can just WRITE!
//    xstream.setMode(XStream.NO_REFERENCES)

        val envXML = gson.toJson(env)

//    env.getKoin()
        println(envXML)
        val restoredEnv: Environment = gson.fromJson(envXML, Environment::class.java)

        println("running restored simulation")
        restoredEnv.run(10.0)
    }
}