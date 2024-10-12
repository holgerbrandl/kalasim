package org.kalasim.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.kalasim.*
import org.kalasim.misc.createTestSimulation
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.days

class TimeTrafoTests {

    class TimeTrafoTestEvent(time: SimTime) : Event(time)

    val startDate = Instant.parse("2021-01-24T12:00:00.00Z")

    @Test
    fun `it should preserve precision when transforming ticks to wall time`() = createTestSimulation(startDate) {
        asSimTime(15.tt) shouldNotBe asSimTime(15.32.tt)

        asSimTime(15.tt) shouldBe Instant.parse("2021-01-24T12:15:00Z")

        300.seconds.asTicks() shouldNotBe 350.seconds.asTicks()
        30.seconds.asTicks() shouldBe 0.5
    }

    @Test
    fun `it should correctly project simulation times with offset-trafo`() = createTestSimulation(startDate) {
        object : Component() {
            override fun process() = sequence {

                addEventListener {
                    if(it !is TimeTrafoTestEvent) return@addEventListener

                    println("tick time is $now")

                    now shouldBe Instant.parse("2021-01-24T13:30:00.00Z")
                }

                hold(90.minutes)

                asSimTime(now.toTickTime()) shouldBe now

                log(TimeTrafoTestEvent(now))
            }
        }

        run(30.days)
        println()
    }


    @Test
    fun `it should correctly project real time simulation ticks to the current wall time`() = createTestSimulation {
        // implement me
    }
}