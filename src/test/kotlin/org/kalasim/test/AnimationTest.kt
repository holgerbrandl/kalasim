package org.kalasim.test
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.animation.AnimationComponent
import java.awt.geom.Point2D

class AnimationTest {

    @Test
    fun`it should allow querying different hold fractions`() = createTestSimulation {

        val o1 = object : AnimationComponent(Point2D.Double(0.0,0.0)){
            override fun process() = sequence {
                hold(10, "h1")
                hold(10, "h2")
                hold(10, "h3")
            }
        }
        // mix in some holds with the same name but unrelated component
        object : Component(){
            override fun process() = sequence {
                hold(2)
                hold(10, "h1")
                hold(10, "h2")
                hold(10, "h3")
            }
        }

        o1.registerHoldTracker("h2"){
            description=="h2"
        }

        run(8)
        o1.holdProgress("h2") shouldBe null
        run(2)
        o1.holdProgress("h2") shouldBe 0.0.plusOrMinus(0.001)
        run(2)
        o1.holdProgress("h2") shouldBe 0.2.plusOrMinus(0.001)
        run(8)
        o1.holdProgress("h2") shouldBe 1.0
        run(1)
        o1.holdProgress("h2") shouldBe null
    }
}

