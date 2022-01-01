package org.kalasim.webui

import com.github.holgerbrandl.jsonbuilder.json
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.kalasim.ComponentListStatistics
import org.kalasim.QueueStatistics
import org.kalasim.examples.MM1Queue

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val moshi = Moshi.Builder()
        .add(CLStatsAdapter())
        .addLast(KotlinJsonAdapterFactory())
//            .add(ComponentListStatistics::class.java, JsonAdapter<CLStatsAdapter>{})
        .build()

    val adapter = moshi.adapter<QueueStatistics>()
    val stats = MM1Queue().apply { run(100) }.server.requesters.stats
    val toJson = adapter.toJson(stats)

    print(toJson)
}


class CLStatsAdapter {

    @ToJson
    fun toJson(cls: ComponentListStatistics): String {
        return with(cls) {
            json {
                "name" to name
                "timestamp" to timestamp.value
                "type" to ComponentListStatistics::class.java.simpleName //"queue statistics"

                "length_of_stay" to {
                    "all" to lengthOfStayStats.toJson()
                    "excl_zeros" to lengthOfStayStatsExclZeros.toJson()
                }

                "size" to {
                    "all" to sizeStats.toJson()
                    "excl_zeros" to sizeStatsExclZeros.toJson()
                }
            }.toString(4)
        }
    }
}
