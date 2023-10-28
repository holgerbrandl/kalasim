package org.kalasim.misc

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.datetime.Instant
import org.json.JSONObject
import java.lang.reflect.Type


interface WithJson {
    fun toJson(): JSONObject
}


/** A component that can be displayed as a string. This is not a serialization format. It solves an analytical purpose only. */
abstract class Jsonable : WithJson {

    abstract override fun toJson(): JSONObject

    override fun toString(): String {
        return toJson().toString(JSON_INDENT)
    }

    fun printJson() = println(toString())
}

internal fun Any.buildJsonWithGson() = JSONObject(GSON.toJson(this))


/** Automatic json serialization with GSON. This works just if the entity to be serialized is using basic types only.*/
open class AutoJson : Jsonable() {
    override fun toJson(): JSONObject {
        return buildJsonWithGson()
    }

    // todo get rid of gson here to simplify dependency tree
//        return GSON.toJson(this)
//        return Json.encodeToString(this)
}

//var ser: JsonSerializer<TickTime> =
//    JsonSerializer<TickTime> { src: TickTime?, _: Type?, _: JsonSerializationContext? ->
////        if(src == null) null else JsonPrimitive(JSON_DF.format(  src.value)) as JsonElement
//        if(src == null) null else JsonPrimitive(src.value.roundAny(2))
//    }

//internal class TickTimeGsonAdapter : TypeAdapter<TickTime>() {
//    override fun write(out: JsonWriter?, tickTime: TickTime?) {
//        out!!.apply {
//            beginObject()
//            value(JSON_DF.format(tickTime!!.value))
////            JsonPrimitive(JSON_DF.format(tickTime!!.value))
//            JsonPrimitive(tickTime.value.roundAny(2))
////            jsonValue(JSON_DF.format(tickTime!!.value))
//            endObject()
//        }
//        return;
//    }
//
//    override fun read(`in`: JsonReader?): TickTime {
//        TODO("Not yet implemented")
//    }
//}

//internal class ComponentGsonAdapter : TypeAdapter<Component>() {
//    override fun write(out: JsonWriter?, value: Component?) {
//        out!!.apply {
//            beginObject()
////            jsonValue(value?.name)
//            name("name").value(value?.name)
//            endObject()
//
//        }
//        return;
//
//    }
//
//    override fun read(`in`: JsonReader?): Component {
//        TODO("Not yet implemented")
//    }
//}
//
//internal class SimEntityGsonAdapter : TypeAdapter<SimulationEntity>() {
//    override fun write(out: JsonWriter?, value: SimulationEntity?) {
////        out!!.jsonValue(json { "name" to value!!.name }.toString())
//        out!!.apply {
//            beginObject()
//            name("name").value(value!!.name)
//            endObject()
//        }
//        return;
//    }
//
//    override fun read(`in`: JsonReader?): SimulationEntity {
//        TODO("Not yet implemented")
//    }
//}

// https://futurestud.io/tutorials/gson-builder-special-values-of-floats-doubles
// https://github.com/google/gson/blob/master/UserGuide.md#null-object-support
internal val GSON by lazy {
    GsonBuilder()
        .serializeSpecialFloatingPointValues()
        .setPrettyPrinting()
        // essentially gson does not seem to support polymorphic serialization https://stackoverflow.com/a/19600090/590437
//        .registerTypeAdapter(SimulationEntity::class.java, SimEntityGsonAdapter())
        .registerTypeAdapter(Instant::class.java, GsonInstantTypeAdapter())
//        .registerTypeAdapter(Instant::class.java, GsonInstantSerializer())
        .serializeNulls().create()
}

var JSON_INDENT = 2

fun JSONObject.toIndentString(): String = toString(JSON_INDENT)


internal class GsonInstantTypeAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter?, value: Instant?) {
        if (value == null) {
            out?.nullValue()
        } else {
//            out?.jsonValue("\""+value.toString()+"\"")
            out?.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader?): Instant {
        TODO("Not yet implemented")
    }
}


private class GsonInstantSerializer : JsonSerializer<Instant> {
    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }
}