package org.kalasim.dokka

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.junit.Test
import org.kalasim.Environment
import org.kalasim.asCMPairList
import org.kalasim.monitors.CategoryTimeline


// Adopted from example in https://www.salabim.org/manual/Monitor.html
fun freqLevelDemo() {
    val data = listOf(
        "foo" to 0.1,
        "bar" to 0.9
    )

    val dist = EnumeratedDistribution(data.asCMPairList())

    val dm = CategoryTimeline("bla")

    repeat(1000) {
        dm.addValue(dist.sample());
        dm.env.run(1)
    }

    dm.printHistogram()

    // get a value at a specific time
    dm[4.5]
}


class DokkaTest {
    @Test
    fun `it should run all dokka examples without exception`() {
        Environment()
        freqLevelDemo()
    }
}