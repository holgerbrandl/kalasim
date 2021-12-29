package org.kalasim.test

import org.junit.Test
import org.kalasim.ComponentList
import org.kalasim.ComponentQueue
import org.kalasim.misc.printThis

class ComponentListTest {

    @Test
    fun `it should gather correct stats`() = createTestSimulation {
        val cl = ComponentList<String>()

        run(1)
        cl.add("foo")
        run(1)
        cl.add("bar")
        run(2)
        cl.remove("bar")

        cl.lengthOfStayStatistics.statistics().printThis()
        cl.printStats()
    }
}
class ComponentQueueTests {

    @Test
    fun `it should gather correct stats`() = createTestSimulation {
        val cl = ComponentQueue<String>()

        run(1)
        cl.add("foo")
        run(1)
        cl.add("bar")
        run(2)
        cl.remove("bar")

        cl.sizeTimeline.statisticsSummary().apply {

        }
        cl.lengthOfStayStatistics.statistics().printThis()
        cl.printStats()
    }
}