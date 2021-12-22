package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.plot.kravis.display


//**TODO**  may we should also test that take works with honorAll?

class DepletableResourceTests {

    @Test
    fun `it should realize depletable resource semantics`() = createTestSimulation(true) {
        DepletableResource(capacity = 100, initialLevel = 0).apply {
            isFull shouldBe false
            isDepleted shouldBe true
            level shouldBe 0
            claimed shouldBe capacity
        }
    }

    @Test
    fun `it should allow to consume and refill a depletable resource`() = createTestSimulation(true) {

        // add new gas continuously
        object : Component() {
            override fun process() = sequence {
                val gasSupply = DepletableResource(capacity = 100, initialLevel = 0)

//                gasSupply.put()
                put(gasSupply, 10)
                take(gasSupply, 10)

                gasSupply.level shouldBe 0

                put(gasSupply, 10)
                put(gasSupply, 10)
                put(gasSupply, 10)

                gasSupply.level shouldBe 30

                shouldThrow<CapacityLimitException> {
                    put(gasSupply, 1000, capacityLimitMode = CapacityLimitMode.FAIL)
                }

                gasSupply.level shouldBe 30

                // Test cap mode
                put(gasSupply, 1000, capacityLimitMode = CapacityLimitMode.CAP)
                gasSupply.level shouldBe 100

                take(gasSupply, 80)

                // Test schedule mode
                put(gasSupply, 1000, capacityLimitMode = CapacityLimitMode.SCHEDULE, failAt = 10.tt)
                now shouldBe 10.tt
                gasSupply.level shouldBe 20

                // but it should allow to max out capacity manually
                put(gasSupply, gasSupply.capacity - gasSupply.level)
                gasSupply.level shouldBe 100

                //make sure to not allow consuming more than fill level
                val levelBefore = gasSupply.level
                shouldThrow<CapacityLimitException> {
                    take(gasSupply, 200)
                }
                levelBefore shouldBe gasSupply.level


                // allow to consume the entire level
                take(gasSupply, gasSupply.level)
                gasSupply.level shouldBe 0
            }
        }

        run()
    }


    @Test
    fun `it allow filling and emptying from 0 to capacity limit`() = createTestSimulation(true) {
        val gasSupply = DepletableResource(capacity = 100, initialLevel = 0)

        gasSupply.isFull shouldBe false
        gasSupply.isDepleted shouldBe true

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
                request(gasSupply withQuantity this@Car.tankSize) {
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
        cg.history.first().componentState shouldBe ComponentState.DATA
    }


    @Test
    fun `it ensure that capacity=INF has meaningful semantics`() = createTestSimulation(true) {
        val dp = DepletableResource(capacity = Double.MAX_VALUE, initialLevel = 0)

        object : Component() {
            override fun process() = sequence {
                repeat(100) {
                    put(dp, 10)
                }

                dp.occupancy shouldBe 0.0.plus(1e-10)
                dp.level shouldBe 1000
            }
        }
    }


    @Test
    fun `it should ensure that a level increase is stalled until capacity becomes available`() =
        createTestSimulation(true) {
            val dp = DepletableResource(capacity = 100, initialLevel = 0)

            dp.level shouldBe 0

            val c = object : Component() {
                override fun process() = sequence<Component> {
                    put(dp, 200, capacityLimitMode = CapacityLimitMode.SCHEDULE)

                    now shouldBe 20.tt
                    dp.level shouldBe 200
                }
            }

            run(20)
            dp.capacity = 500.0
            run(1)

            queue shouldNotContain c
        }


    @Test
    fun `it should respect queue priorities when consuming resource`() = createTestSimulation(true) {
        val dp = DepletableResource(capacity = 100, initialLevel = 0)

        dp.level shouldBe 0

        val c1 = object : Component() {
            override fun process() = sequence {
                take(dp, 50)
            }
        }

        val c2 = object : Component() {
            override fun process() = sequence {
                take(dp, 50, priority = Priority.IMPORTANT)
            }
        }

        object : Component() {
            override fun process() = sequence {
                put(dp, 80)
                c1.componentState shouldBe ComponentState.REQUESTING
                c2.componentState shouldBe ComponentState.SCHEDULED
            }
        }

        run()
        dp.level shouldBe 30.0
    }


    @Test
    fun `it should respect queue priorities when restoring resource`() = createTestSimulation(true) {
        val resource = DepletableResource(capacity = 10, initialLevel = 7)

        var normalPutHonored = false

        object : Component("EarlyProvider") {
            override fun process() = sequence {
                    // this put will be stalled because there it is currently fully charged
                    put(resource, 7, description = "bla bla", capacityLimitMode = CapacityLimitMode.SCHEDULE)
                    normalPutHonored = true
            }
        }

        var prioPutHonored = false

        object : Component("PrioProvide") {
            override fun process() = sequence {
                // this put will be stalled because there it is currently fully charged
                put(resource, 7, Priority.IMPORTANT, capacityLimitMode = CapacityLimitMode.SCHEDULE)
                prioPutHonored = true
            }
        }

        object : Component("Consumer") {
            override fun process() = sequence {
                hold(duration = 5.0)
                take(resource, 5)
            }
        }

        run(10)

        normalPutHonored shouldBe false
        prioPutHonored shouldBe true
    }
}