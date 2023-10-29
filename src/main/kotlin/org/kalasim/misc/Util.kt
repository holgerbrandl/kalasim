package org.kalasim.misc

import kotlinx.coroutines.*
import org.apache.commons.math3.util.Precision
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


internal var JSON_DF = DecimalFormat("###.00", DecimalFormatSymbols(Locale.ENGLISH))

// move away from main namespace
internal var TRACE_DF = JSON_DF


@Suppress("FunctionName")
internal fun ImplementMe(): Nothing =
    TODO("Not yet implemented. Please file a ticket under https://github.com/holgerbrandl/kalasim/issues")


fun Any.printThis() {
    println(toString())
}


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
fun <T> repeat(n: Int, builder: (Int) -> T) = (1..n).map { builder(it) }


// https://stackoverflow.com/questions/48007311/how-do-i-infinitely-repeat-a-sequence-in-kotlin
fun <T> Iterable<T>.repeat() = sequence {
    while(true) yieldAll(this@repeat)
}


//
// Parallel collection utilities
//

fun <A, B> Iterable<A>.fastMap(
//    numThreads: Int = Runtime.getRuntime().availableProcessors(),
    f: suspend (A) -> B
): List<B> = runBlocking {
    map { async(Dispatchers.Default) { f(it) } }.map { it.await() }
}

@Suppress("unused")
fun <A, B> Iterable<A>.fastForEach(
//    numThreads: Int = Runtime.getRuntime().availableProcessors(),
    f: suspend (A) -> B
) {
    fastMap(f).count() // count is just needed to force computations
}


/** The environment mode also allows you to detect common bugs in your implementation. */
enum class AssertMode {
    /** Productive mode, where asserts that may impact performance are disabled. */
    @Suppress("unused") // we may want to add some test-coverage for that one as well
    OFF,

    /** Disables compute-intensive asserts. This will have a minimal to moderate performance impact on simulations. */
    LIGHT,

    /** Full introspection, this will have a measurable performance impact on simulations. */
    FULL
}

var ASSERT_MODE = AssertMode.LIGHT

//fun Double?.roundAny(n: Int = 3) = if (this == null) this else Precision.round(this, n)
fun Double.roundAny(n: Int = 3) = Precision.round(this, n)

/**
 * Replacement for Kotlin's deprecated `capitalize()` function.
 *
 * From https://stackoverflow.com/questions/67843986/is-there-a-shorter-replacement-for-kotlins-deprecated-string-capitalize-funct
 */
fun String.titlecaseFirstChar() = replaceFirstChar(Char::titlecase)

typealias CommonMathPair<K, V> = org.apache.commons.math3.util.Pair<K, V>

fun <T, S> List<Pair<T, S>>.asCMPairList(): List<CommonMathPair<T, S>> = map { CommonMathPair(it.first, it.second) }

fun <T, S> Map<T, S>.asCMPairList(): List<CommonMathPair<T, S>> = map { CommonMathPair(it.key, it.value) }