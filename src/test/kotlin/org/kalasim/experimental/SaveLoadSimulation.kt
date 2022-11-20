package org.kalasim.experimental

import com.google.gson.Gson
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.AnyTypePermission
import org.kalasim.*
import org.kalasim.examples.MM1Queue
import java.util.*

object XStreamPersistence {
    @JvmStatic
    fun main(args: Array<String>) {
        class Foo : Component()

//        val env = createSimulation {
//            Foo()
//            Resource()
//        }

        val env = MM1Queue().apply { run(10) }

        // save it to xml
        //    https://github.com/x-stream/xstream/issues/101
        val xstream = XStream()
//    val xstream = XStream(JsonHierarchicalStreamDriver())--> does not work because embeed json driver can just WRITE!
//    xstream.setMode(XStream.NO_REFERENCES)

        val envXML = xstream.toXML(env)

//    env.getKoin()
        println(xstream.toXML(env))
        xstream.addPermission(AnyTypePermission.ANY) // to prevent https://stackoverflow.com/questions/30812293/com-thoughtworks-xstream-security-forbiddenclassexception
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

        // fails because of https://stackoverflow.com/questions/43410654/gson-tojson-throws-stackoverflowerror-in-servlet
        val envXML = gson.toJson(env)

//    env.getKoin()
        println(envXML)
        val restoredEnv: Environment = gson.fromJson(envXML, Environment::class.java)

        println("running restored simulation")
        restoredEnv.run(10.0)
    }
}

object XStreamPQ {
    @JvmStatic
    fun main(args: Array<String>) {
        val q: Queue<String> = PriorityQueue { o1, o2 ->
            compareValuesBy(
                o1,
                o2,
                { it.length },
                { it.last() })
        }


        q.add("foo")

        val xstream = XStream()
        val envXML = xstream.toXML(q)

        xstream.addPermission(AnyTypePermission.ANY) // to prevent https://stackoverflow.com/questions/30812293/com-thoughtworks-xstream-security-forbiddenclassexception
        @Suppress("UNCHECKED_CAST") val qRestored: PriorityQueue<String> = xstream.fromXML(envXML) as PriorityQueue<String>

        qRestored.add("bar")

        println(qRestored)

    }
}


object XStreamPQ2 {
    @JvmStatic
    fun main(args: Array<String>) {
        val q: Queue<String> = PriorityQueue { o1, o2 ->
            compareValuesBy(
                o1,
                o2,
                { it.length },
                { it.last() })
        }


        q.add("foo")

        val xstream = XStream()
        val envXML = xstream.toXML(q)
        println(envXML)

        xstream.addPermission(AnyTypePermission.ANY) // to prevent https://stackoverflow.com/questions/30812293/com-thoughtworks-xstream-security-forbiddenclassexception

        @Suppress("UNCHECKED_CAST") val qRestored: PriorityQueue<String> = xstream.fromXML(envXML) as PriorityQueue<String>


        qRestored.add("bar")

        println(qRestored)

    }
}

