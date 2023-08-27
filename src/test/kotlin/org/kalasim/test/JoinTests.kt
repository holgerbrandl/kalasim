package org.kalasim.test

import org.kalasim.enableEventLog
import org.kalasim.examples.shipyard.Shipyard
import org.kalasim.weeks

class JoinTests {

    @Test
    fun `it should join processes at ease`() {
        val shipyard = Shipyard().apply {
            enableEventLog()
            run(1.weeks)
        }

        shipyard.get<EventLot>().fil
    }
}