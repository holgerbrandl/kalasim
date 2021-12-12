package org.kalasim.test

import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.*
import org.kalasim.*
import org.kalasim.ResourceSelectionPolicy.*
import org.kalasim.plot.kravis.display

class DepletableResourceTests {

    @Test
    fun `it allow filling and emptying from 0 to capacity limit`() = createTestSimulation(true) {
        val gasSupply = DepletableResource(capacity = 10)

        // add new gas continuously
        object : Component("Pipeline") {
            override fun repeatedProcess() = sequence {
                hold(1, "refilling supply tank")
                gasSupply.level += 2
            }
        }

        val tankSize = discreteUniform(10, 100)

        class Car(val tankSize: Int): Component() {
            override fun process() = sequence {
                request(gasSupply withQuantity this@Car.tankSize)
            }
        }

        val cg = ComponentGenerator(iat = exponential(10), keepHistory = true) { Car(tankSize()) }

        run(100)

        with(gasSupply){
            claimers.size shouldBe 0
            level shouldBeGreaterThan 0.0
            level shouldBeLessThan  0.0

            claimedTimeline.display("supply fill status").show()
        }

        // ensure that at least the first car was sucessfully refilled
        cg.history.first().componentState shouldBe ComponentState.PASSIVE
    }
}

fun main() {
    DepletableResourceTests().`it allow filling and emptying from 0 to capacity limit`()
    Thread.sleep(10000)
}


