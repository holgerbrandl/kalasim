package org.kalasim.misc

import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.text.DecimalFormat


var JSON_DF = DecimalFormat("###.00")

// move away from main namespace
var TRACE_DF = DecimalFormat("###.00")


internal fun Any.printThis() {
    println(toString())
}


internal fun ImplementMe(): Nothing =
    TODO("Not yet implemented. Please file a ticket under https://github.com/holgerbrandl/kalasim/issues")


//@Serializable
abstract class Jsonable {

    open fun toJson(): JSONObject =
        JSONObject(GSON.toJson(this))

    override fun toString(): String {
        return toJson().toString(JSON_INDENT)

        // todo get rid of gson here to simplify dependency tree
//        return GSON.toJson(this)
//        return Json.encodeToString(this)
    }
}

// https://futurestud.io/tutorials/gson-builder-special-values-of-floats-doubles
// https://github.com/google/gson/blob/master/UserGuide.md#null-object-support
internal val GSON by lazy {
    GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().serializeNulls().create()
}

var JSON_INDENT = 2


// from https://stackoverflow.com/questions/46895140/how-to-perform-action-for-all-combinations-of-elements-in-lists
// ternary and higher order: https://stackoverflow.com/questions/53749357/idiomatic-way-to-create-n-ary-cartesian-product-combinations-of-several-sets-of // todo simply add up to 8ary?
/** Calculate all pairwise combination of 2 lists.*/
fun <A, B> cartesianProduct(
    listA: Iterable<A>,
    listB: Iterable<B>
): Sequence<Pair<A, B>> =
    sequence {
        listA.forEach { a ->
            listB.forEach { b ->
                yield(a to b)
            }
        }
    }

//https://kotlinlang.slack.com/archives/C0B8Q383C/p1610224701103600
// Same can be achieved with List(10){ foo() }
fun <T> repeat(n:Int, builder: (Int) -> T) = (1..n).map{ builder(it)}


// https://stackoverflow.com/questions/48007311/how-do-i-infinitely-repeat-a-sequence-in-kotlin
fun <T> Iterable<T>.repeat() = sequence {
    while (true) yieldAll(this@repeat)
}


/** The environment mode also allows you to detect common bugs in your implementation. */
enum class AssertMode{
    /** Productive mode, where asserts that may impact performance are disabled. */
    OFF,
    /** Disables compute-intensive asserts. This will have a minimal to moderate performance impact on simulations. */
    LIGHT,
    /** Full introspection, this will have a measurable performance impact on simulations. */
    FULL
}

var ASSERT_MODE = AssertMode.LIGHT
