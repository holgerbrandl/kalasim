package org.kalasim.scratch

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.kalasim.Resource
import kotlin.reflect.KFunction1


typealias FunPointer = KFunction1<Double, Sequence<Double>>

@JsonClass(generateAdapter = true)
open class Something(
    myName: String? = null,

    // fails to compile with
    process: FunPointer? =null
    // compiles with
//    process: KFunction1<Double, Sequence<Double>>? =null
){
    internal val requests = mapOf<Resource, Double>().toMutableMap()

    internal var waitAll: Boolean = false

    val myProcess = process
}


fun plus2 (x:Double) =  sequence { yield(x+2)  }
fun plus3 (x:Double) =  sequence { yield(x+3)  }

fun main() {
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(Something::class.java)

    val foo = Something("bar", process= ::plus2)

    val toJson = adapter.toJson(foo)
    println(toJson)
    val restored = adapter.fromJson(toJson)

    println("original: ${foo.myProcess?.let { it(1.0).first() }}")
    println("restored: ${restored!!.myProcess?.let { it(1.0).first() }}")
}