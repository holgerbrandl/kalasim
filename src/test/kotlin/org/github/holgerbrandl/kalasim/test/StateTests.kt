package org.github.holgerbrandl.kalasim.test

import org.github.holgerbrandl.kalasim.State
import org.github.holgerbrandl.kalasim.StateRequest
import org.junit.Test

class StateTests {

    @Test
    fun testPredicate() {
        val (state, _, predicate) = StateRequest(State("foo")) { it.value == "House" }
        predicate(state)
    }
}