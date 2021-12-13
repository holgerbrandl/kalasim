package org.kalasim.test

import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import junit.framework.Assert.fail
import org.junit.*
import org.kalasim.*
import org.kalasim.ResourceSelectionPolicy.*
import org.kalasim.plot.kravis.display

class DepletableResourceTests {

    @Test
    fun `it allow filling and emptying from 0 to capacity limit`() = createTestSimulation(true) {
        val gasSupply = DepletableResource(capacity = 100, initialLevel = 0)

        // add new gas continuously
        object : Component("Pipeline") {
            override fun repeatedProcess() = sequence {
                hold(10, "refilling supply tank")

                put(gasSupply withQuantity 2)
                println("new level is ${gasSupply.level}")
            }
        }

        val tankSize = discreteUniform(10, 100)

        class Car(val tankSize: Int) : Component() {
            override fun process() = sequence {
                request(gasSupply withQuantity this@Car.tankSize){
                    println("refilled ${this@Car}")
                }
            }
        }

        val cg = ComponentGenerator(iat = exponential(10), keepHistory = true) { Car(tankSize()) }

        run(1000)

        with(gasSupply) {
            claimers.size shouldBe 0
            level shouldBeGreaterThan 0.0

            claimedTimeline.display().show()
            levelTimeline.display().show()
        }

        TickTime(Double.POSITIVE_INFINITY).toString()
        // ensure that at least the first car was sucessfully refilled
        cg.history.first().componentState shouldBe ComponentState.PASSIVE
    }

    @Ignore
    @Test
    fun `it ensure that capacity=INF has meaninful semantic`() = createTestSimulation(true) {
        fail()
    }

    @Ignore
    @Test
    fun `it should ensure that a level incrase is stalled until capacity becomes available`() = createTestSimulation(true) {
        fail()
    }

    @Ignore
    @Test
    fun `it should respect queue priorities when consuming resource`() = createTestSimulation(true) {
        fail()
    }

    @Ignore
    @Test
    fun `it should respect queue priorities when restoring resource`() = createTestSimulation(true) {
        fail()
    }
}

fun main() {
    DepletableResourceTests().`it allow filling and emptying from 0 to capacity limit`()
    Thread.sleep(10000)
}


