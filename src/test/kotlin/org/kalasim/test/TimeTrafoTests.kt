package org.kalasim.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.kalasim.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class TimeTrafoTests {

    class TimeTrafoTestEvent(time: Double) : Event(time)


    @Test
    fun `it should preserve precision when transforming ticks to walltime`() = createTestSimulation {
        val baseTime = Instant.parse("2021-01-24T12:00:00.00Z")

        tickTransform = OffsetTransform(baseTime, TimeUnit.MINUTES)

        asWallTime(15.0) shouldNotBe asWallTime(15.32)

        asWallTime(15.0) shouldBe  Instant.parse("2021-01-24T12:15:00Z")

        Duration.ofSeconds(300).asTicks() shouldNotBe Duration.ofSeconds(350).asTicks()
        Duration.ofSeconds(30).asTicks() shouldBe 0.5

    }

    @Test
    fun `it should correctly project simulation times with offset-trafo`() = createTestSimulation(true) {
        val baseTime = Instant.parse("2021-01-24T12:00:00.00Z")

        tickTransform = OffsetTransform(baseTime, TimeUnit.MINUTES)

        object : Component() {
            override fun process() = sequence {

                addEventListener {
                    if(it !is TimeTrafoTestEvent) return@addEventListener

                    println("tick time is ${asWallTime(now)}")

                    asWallTime(now) shouldBe asWallTime(it.time)
                    asWallTime(now) shouldBe Instant.parse("2021-01-24T13:30:00.00Z")
                }

                hold(asTicks(Duration.ofMinutes(90)))
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