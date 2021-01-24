package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.Event
import org.kalasim.OffsetTransform
import org.kalasim.TickTransform
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class TimeTrafoTests {

    class TimeTrafoTestEvent(time: Double) : Event(time)

    @Test
    fun `it should project ticks to the real world clock with custon trafo`() = createTestSimulation {
        val baseTime = Instant.parse("2021-01-24T11:16:00.00Z")

        object : Component() {
            override fun process() = sequence<Component> {
                tickTransform = TickTransform { baseTime + Duration.ofSeconds(now.toLong()) }

                addEventListener {
                    transformTickTime(now) shouldBe transformTickTime(it.time)
                }

                hold(100)
                log(TimeTrafoTestEvent(now))
            }
        }

        run(1000)
    }

    @Test
    fun `it should correctly project simulation times with offset-trafo`() = createTestSimulation(true) {
        val baseTime = Instant.parse("2021-01-24T11:16:00.00Z")

        object : Component() {
            override fun process() = sequence<Component> {
                tickTransform = OffsetTransform(baseTime, TimeUnit.MINUTES)

                addEventListener {
                    transformTickTime(now) shouldBe transformTickTime(it.time)
                    println("tick time is ${transformTickTime(now)}")
                }

                hold(100)
                log(TimeTrafoTestEvent(now))
            }
        }

        run(1000)
    }


    @Test
    fun `it should correctly project real time simulation ticks to the current wall time`() = createTestSimulation {


    }
}