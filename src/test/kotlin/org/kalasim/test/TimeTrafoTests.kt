package org.kalasim.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.kalasim.*
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeTrafoTests {

    class TimeTrafoTestEvent(time: TickTime) : Event(time)


    @Test
    fun `it should preserve precision when transforming ticks to wall time`() = createTestSimulation {
        startDate = Instant.parse("2021-01-24T12:00:00.00Z")

        asWallTime(15.tt) shouldNotBe asWallTime(15.32.tt)

        asWallTime(15.tt) shouldBe Instant.parse("2021-01-24T12:15:00Z")

        300.seconds.asTicks() shouldNotBe 350.seconds.asTicks()
        30.seconds.asTicks() shouldBe 0.5
    }

    @Test
    fun `it should correctly project simulation times with offset-trafo`() = createTestSimulation(true) {
        startDate = Instant.parse("2021-01-24T12:00:00.00Z")

        object : Component() {
            override fun process() = sequence {

                addEventListener {
                    if (it !is TimeTrafoTestEvent) return@addEventListener

                    println("tick time is ${asWallTime(now)}")

                    asWallTime(now) shouldBe asWallTime(it.time)
                    asWallTime(now) shouldBe Instant.parse("2021-01-24T13:30:00.00Z")
                }

                hold(asTicks(90.minutes))

                toTickTime(now.asWallTime()) shouldBe now

                log(TimeTrafoTestEvent(now))
            }
        }

        run(10000)
        println()
    }


    @Test
    fun `it should correctly project real time simulation ticks to the current wall time`() = createTestSimulation {
        // implement me
    }
}