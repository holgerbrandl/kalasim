package org.kalasim.test

import org.junit.Test
import org.kalasim.Component
import org.kalasim.misc.println

class ComponentTests {

    @Test
    fun `it should create components outside of an environment`() {
        Component("foo").info.println()
    }
}