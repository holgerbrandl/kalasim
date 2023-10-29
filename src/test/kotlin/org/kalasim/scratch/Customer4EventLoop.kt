package org.kalasim.scratch.eventloop

import org.kalasim.Component
import org.kalasim.Resource
import kotlin.time.Duration.Companion.minutes

class Customer(val clerk: Resource) : Component() {

    override fun process() = sequence {
        hold(30.minutes, "do shopping")

        request(clerk) {
            hold(2.minutes, "billing")
        }
    }
}