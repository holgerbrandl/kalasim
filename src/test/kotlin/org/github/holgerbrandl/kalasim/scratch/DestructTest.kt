package org.github.holgerbrandl.kalasim.scratch

//https://kotlinlang.org/docs/reference/generics.html#star-projections

//https://stackoverflow.com/questions/40138923/difference-between-and-any-in-kotlin-generics

class DestructState<T>(initialValue: T) {
    var value: T = initialValue
}

data class DestructStateRequest<T>(val state: DestructState<T>, val predicate: (DestructState<T>) -> Boolean, val priority: Int? = null)

fun main() {
//    val stateRequest: StateRequest<*> = StateRequest(State("foo"), {
    val stateRequest: DestructStateRequest<Any> = DestructStateRequest(DestructState("foo"), {
        println("predicate called")
        true
    })

    val predicate  = stateRequest.predicate


    // destructure it
    val (state, predicate2, priority) = stateRequest


    // call it the hard way with lots of casting
    (stateRequest as DestructStateRequest<Any>).predicate(state as DestructState<Any>)

    predicate(state)
}
