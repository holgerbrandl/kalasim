package org.kalasim.misc

import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.junit.Test
import org.kalasim.Environment
import org.kalasim.FrequencyLevelMonitor


// Adopted from example in https://www.salabim.org/manual/Monitor.html
fun freqLevelDemo() {
    val data = listOf(
        "foo" to 0.1,
        "bar" to 0.9
    )

    val dist = EnumeratedDistribution(data.asCMPairList())

    val dm = FrequencyLevelMonitor("bla")

    repeat(1000) { dm.addValue(dist.sample()); dm.env.now += 1 }

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