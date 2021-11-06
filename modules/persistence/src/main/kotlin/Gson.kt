package org.kalasim.examples.hospital

import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.kalasim.*
import org.kalasim.demo.MM1Queue


object GsonComponent{
    @JvmStatic
    fun main(args: Array<String>) {

            createSimulation {  }
            val c = Component()


        //Gson gson = new Gson();
        //Gson gson = new Gson();
        val gson = GsonBuilder().setPrettyPrinting().create()


        val jsonString = gson.toJson(c)

        val restored: Component = gson.fromJson(jsonString, Component::class.java)

            println("name is ${restored!!.name}")
    }
}

object GsonResource{
    @JvmStatic
    fun main(args: Array<String>) {

            createSimulation {  }
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
