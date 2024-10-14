package org.kalasim.examples.taxiinc

import com.google.gson.GsonBuilder
import org.json.JSONObject


internal val GSON by lazy {
    GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .setPrettyPrinting()
        // essentially gson does not seem to support polymorphic serialization https://stackoverflow.com/a/19600090/590437
//        .registerTypeAdapter(SimulationEntity::class.java, SimEntityGsonAdapter())
//        .registerTypeAdapter(TickTime::class.java, ser)
        .serializeNulls().create()
}

internal fun Any.buildJsonWithGson() = JSONObject(GSON.toJson(this))
