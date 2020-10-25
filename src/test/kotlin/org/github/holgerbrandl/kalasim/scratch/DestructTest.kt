package org.github.holgerbrandl.kalasim.scratch

//https://kotlinlang.org/docs/reference/generics.html#star-projections

//https://stackoverflow.com/questions/40138923/difference-between-and-any-in-kotlin-generics

class State<T>(initialValue: T) {
    var value: T = initialValue
}

data class StateRequest<T>(val state: State<T>, val predicate: (State<T>) -> Boolean, val priority: Int? = null)

fun main() {
//    val stateRequest: StateRequest<*> = StateRequest(State("foo"), {
    val stateRequest: StateRequest<Any> = StateRequest(State("foo"), {
        println("predicate called")
        true
    })

    val predicate  = stateRequest.predicate


    // destructure it
    val (state, predicate2, priority) = stateRequest


    // call it the hard way with lots of casting
    (stateRequest as StateRequest<Any>).predicate(state as State<Any>)

    predicate(state)
}
