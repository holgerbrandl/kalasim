package org.kalasim.webui

import com.github.holgerbrandl.jsonbuilder.json
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.kalasim.QueueStatistics
import org.kalasim.examples.MM1Queue


fun main() {

    val builder = GsonBuilder()
    builder.registerTypeAdapter(QueueStatistics::class.java, GsonCLStatsAdapter())
    builder.setPrettyPrinting()
    builder.serializeSpecialFloatingPointValues()
    val gson = builder.create()

    val stats = MM1Queue().apply { run(100) }.server.requesters.statistics
    val toJson = gson.toJson(stats)

    print(toJson)
}


class GsonCLStatsAdapter : TypeAdapter<QueueStatistics>() {
    override fun write(out: JsonWriter?, value: QueueStatistics?) {
        return with(value!!) {
            json {
                "name" to name
                "timestamp" to timestamp.value
                "type" to value.javaClass.simpleName //"queue statistics"

                "length_of_stay" to {
                    "all" to lengthOfStayStats.toJson()
                    "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
                }

                "queue_length" to {
                    "all" to lengthStats.toJson()
                    "excl_zeros" to lengthStatsExclZeros.toJson()
                }
            }
            .toString(4).let{ out!!.apply{
                jsonValue(it)
            }}
        }

    }

    override fun read(`in`: JsonReader?): QueueStatistics {
        TODO("Not yet implemented")
    }
}
