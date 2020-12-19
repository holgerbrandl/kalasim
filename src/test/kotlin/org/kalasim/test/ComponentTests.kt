package org.kalasim.test

import org.junit.Test
import org.kalasim.Component
import org.kalasim.misc.printThis

class ComponentTests {

    @Test
    fun `it should create components outside of an environment`() {
        Component("foo").info.printThis()
    }

    @Test
    fun `it should track status changes`() = createTestSimulation {
        val component = Component("foo")


        run(2)



    }
}

