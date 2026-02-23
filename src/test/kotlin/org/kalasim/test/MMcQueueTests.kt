@file:OptIn(AmbiguousDuration::class)

package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.kalasim.examples.MMcQueue
import org.kalasim.misc.AmbiguousDuration

class MMcQueueTests {

    @Test
    fun `it should produce a sensible  number of events`(){
        val mMcQueue = MMcQueue(c = 4, mu = 4, lambda = 12)
        mMcQueue.run(1000)

        mMcQueue.componentGenerator.numGenerated shouldBe 12000.0


    }

}