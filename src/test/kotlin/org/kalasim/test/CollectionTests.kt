package org.kalasim.test

import org.junit.Test
import org.kalasim.ComponentList
import org.kalasim.ComponentQueue
import org.kalasim.misc.printThis
import org.kalasim.misc.toIndentString

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
        cl.toString().printThis()
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
        cl.statistics.toJson().toIndentString()
    }
}