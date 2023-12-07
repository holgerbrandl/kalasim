package org.kalasim.logistics

import org.kalasim.createSimulation
import org.kalasim.enableComponentLogger
import kotlin.test.Test
import kotlin.time.Duration.Companion.days

class CollisionAvoidanceTests {

    @Test
    fun `cars must not crash`() {
        createSimulation {
            enableComponentLogger()

            val crossing = Crossing(2, 2, 2, 10)

            CollisionSampler()

            crossing.run(10.days)
        }
    }
}
