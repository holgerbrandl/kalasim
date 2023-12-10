package org.kalasim.test

import io.kotest.matchers.comparables.shouldBeLessThan
import org.junit.Test
import org.kalasim.*
import org.kalasim.examples.shipyard.PartCompleted
import org.kalasim.examples.shipyard.Shipyard
import org.kalasim.misc.*
import kotlin.time.Duration.Companion.hours


class JoinTests {

    @OptIn(AmbiguousDuration::class)
    @Test
    fun `it should join processes at ease`() = createTestSimulation() {
        val c1 = object : Component() {
            override fun process() = sequence {
                hold(10.hours)
            }
        }

        val c2 = object : Component() {
            override fun process() = sequence {
                hold(6.hours)
            }
        }

        object : Component() {
            override fun process() = sequence {
                join(c1, c2)
                require(c1.isData)
                hold(1.hours)
            }
        }

        run()

        now - startDate shouldBe 11.hours
    }


    @Test
    fun `ìt should produced ships in the right order`() = testModel(Shipyard()) {
        configureOrders()
        enableEventLog()

        addEventListener<PartCompleted> {
            if(it.part.finalProduct) {
                stopSimulation()
            }
        }

        run()

        val ship = get<EventLog>().filterIsInstance<PartCompleted>().first { it.part.finalProduct }

        ship.part.computeMinimalMakespan() shouldBeLessThan (now - startDate)
    }
}
