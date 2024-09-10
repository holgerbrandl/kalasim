package org.kalasim.test

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.kalasim.Component
import org.kalasim.animation.AnimationComponent
import org.kalasim.misc.createTestSimulation
import java.awt.geom.Point2D
import kotlin.time.Duration.Companion.seconds

class AnimationTest {

    @Test
    fun `it should allow querying different hold fractions`() = createTestSimulation {

        val o1 = object : AnimationComponent(Point2D.Double(0.0, 0.0)) {
            override fun process() = sequence {
                hold(10.seconds, "h1")
                hold(10.seconds, "h2")
                hold(10.seconds, "h3")
            }
        }
        // mix in some holds with the same name but unrelated component
        object : Component() {
            override fun process() = sequence {
                hold(2.seconds)
                hold(10.seconds, "h1")
                hold(10.seconds, "h2")
                hold(10.seconds, "h3")
            }
        }

        o1.registerHoldTracker("h2") {
            description == "h2"
        }

        run(8.seconds)
        o1.holdProgress("h2") shouldBe null
        run(2.seconds)
        o1.holdProgress("h2") shouldBe 0.0.plusOrMinus(0.001)
        run(2.seconds)
        o1.holdProgress("h2") shouldBe 0.2.plusOrMinus(0.001)
        run(8.seconds)
        o1.holdProgress("h2") shouldBe 1.0
        run(1.seconds)
        o1.holdProgress("h2") shouldBe null
    }
}

