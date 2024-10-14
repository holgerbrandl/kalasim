@file:Suppress("DuplicatedCode")

package org.kalasim.webui

import com.github.holgerbrandl.jsonbuilder.json
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
    val stats = MM1Queue().apply { run(100) }.server.requesters.statistics
    val toJson = adapter.toJson(stats)

    print(toJson)
}


class CLStatsAdapter {

    @ToJson
    fun toJson(cls: ComponentListStatistics): String = with(cls) {
        json {
            "name" to name
            "timestamp" to timestamp.value
            "length_of_stay" to lengthOfStayStats.toJson()
            "size" to sizeStats.toJson()
        }
    }.toString(4)
}
