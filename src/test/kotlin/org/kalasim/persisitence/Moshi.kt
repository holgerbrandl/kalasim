package org.kalasim.examples.hospital

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.kalasim.*
import org.kalasim.demo.MM1Queue
import org.kalasim.misc.DependencyContext
import org.koin.core.Koin
import org.koin.core.logger.EmptyLogger
import org.koin.core.logger.Logger
import java.time.Instant


class KoinLoggerAdapter {
    var moshi = Moshi.Builder().build()

    @ToJson
    fun toJson(field: Logger): String {
        return ""
    }

    @FromJson
    fun fromJson(json: String): Logger {

        return EmptyLogger()
//        throw JsonDataException("Not a valid field JSON")
    }
}

class InstantAdapter {
    var moshi = Moshi.Builder().build()

    @ToJson
    fun toJson(field: Instant): String {
        return field.toEpochMilli().toString()
    }

    @FromJson
    fun fromJson(json: String): Instant {

        return Instant.ofEpochMilli(json.toLong())
//        throw JsonDataException("Not a valid field JSON")
    }
}

class GenProcessInternalAdapter {
    var moshi = Moshi.Builder().build()

    @ToJson
    fun toJson(field: GenProcessInternal): String {
        return ""
    }

    @FromJson
    fun fromJson(json: String): GenProcessInternal {
//        val jsonObject = gson.fromJson(json, JsonObject::class.java)
//        var field: Field? = null
//
//        if (jsonObject.has("name")) {
//            val field1JsonAdapter: JsonAdapter<Field1> = moshi.adapter(Field1::class.java)
//            field = field1JsonAdapter.fromJson(json)
//        } else if (jsonObject.has("address")) {
//            val field2JsonAdapter: JsonAdapter<Field2> = moshi.adapter(Field2::class.java)
//            field = field2JsonAdapter.fromJson(json)
//        }
//
//        field?.let {
//            return it
//        }

        return GenProcessInternal(Component(), sequence { }, "foo")
//        throw JsonDataException("Not a valid field JSON")
    }
}


fun buildMoshi(): Moshi = Moshi.Builder()
    .add(KoinLoggerAdapter())
    .add(GenProcessInternalAdapter())
    .add(InstantAdapter())
    .addLast(KotlinJsonAdapterFactory())
//        .add(SimProcess::class.java)
//                .add(Logger::class.java, JsonAdapter<SimProcess>())
    .build()

object MoshiKoin {
    @JvmStatic
    fun main(args: Array<String>) {

        createSimulation { }
        val c = DependencyContext.startKoin {}.koin


        val moshi = buildMoshi()


        val adapter = moshi.adapter(Koin::class.java)
        val toJson = adapter.toJson(c)
        val restored = adapter.fromJson(toJson)

        println("name is ${restored!!}")
    }
}


object MoshiEnv {
    @JvmStatic
    fun main(args: Array<String>) {
        val env = Environment()
        val moshi = buildMoshi()

        val adapter = moshi.adapter(Environment::class.java)
        val toJson = adapter.toJson(env)
        println(toJson)
        val restored = adapter.fromJson(toJson)

        println("name is ${restored!!}")
    }
}

object MoshiEnvWithComponents {
    @JvmStatic
    fun main(args: Array<String>) {
        val env = Environment().apply {
            dependency { Resource("foo") }
            dependency { State("foo") }
            Component("foo")
            Component("bar")
            run(10)
        }

        println(env.components.get(0))

        val moshi = buildMoshi()

        val adapter = moshi.adapter(Environment::class.java)
        val toJson = adapter.toJson(env)
        println(toJson)
        val restored = adapter.fromJson(toJson)

        println("name is ${restored!!}")
        restored.run(10)
        println("name is ${restored!!}")
    }
}

object MoshiComponent {
    @JvmStatic
    fun main(args: Array<String>) {

        createSimulation { }
        val c = Component()


        val moshi = buildMoshi()

        val adapter = moshi.adapter(Component::class.java)
        val toJson = adapter.toJson(c)
        println(toJson)
        val restoredSim: Component? = adapter.fromJson(toJson)

        println("name is ${restoredSim!!.name}")
    }
}

object MoshiResource {
    @JvmStatic
    fun main(args: Array<String>) {

        createSimulation { }
        val c = Resource()


        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
//        .add(SimProcess::class.java)
//                .add(SimProcess::class.java, JsonAdapter<SimProcess>())
            .build()

        val adapter = moshi.adapter(Resource::class.java)
        val toJson = adapter.toJson(c)
        val restoredSim: Resource? = adapter.fromJson(toJson)

        println("name is ${restoredSim!!.name}")
    }
}

fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }


    val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
//        .add(SimProcess::class.java)
//        .add(SimProcess::class.java,  converter.toJsonAdapter())
        .build()

    val adapter = moshi.adapter(Environment::class.java)
    val toJson = adapter.toJson(sim)
    val restoredSim: Environment? = adapter.fromJson(toJson)


    // analysis
//    sim.testSim()
    restoredSim!!.run(10)

    // visualize room setup as gant chart

}
