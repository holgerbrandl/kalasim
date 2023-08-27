package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.kalasim.examples.shipyard.Shipyard
import org.junit.Test
import org.kalasim.*
import org.kalasim.examples.shipyard.PartCompleted

class JoinTests {

    @Test
    fun `it should join processes at ease`() {
        val shipyard = Shipyard().apply {
            configureOrders()
            enableEventLog()
            run(4.weeks)
        }

        println(shipyard)

        shipyard.get<EventLog>().filterIsInstance<PartCompleted>().isNotEmpty() shouldBe true
    }
}