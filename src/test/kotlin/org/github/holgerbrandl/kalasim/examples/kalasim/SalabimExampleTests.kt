package org.github.holgerbrandl.kalasim.examples.kalasim

import io.kotest.matchers.shouldBe
import org.github.holgerbrandl.kalasim.ComponentQueue
import org.github.holgerbrandl.kalasim.State
import org.github.holgerbrandl.kalasim.add
import org.github.holgerbrandl.kalasim.analytics.display
import org.github.holgerbrandl.kalasim.configureEnvironment
import org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.numBalked
import org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.numReneged
import org.github.holgerbrandl.kalasim.examples.koiner.Clerk
import org.github.holgerbrandl.kalasim.examples.koiner.Customer
import org.github.holgerbrandl.kalasim.examples.koiner.CustomerGenerator
import org.json.JSONObject
import org.koin.core.get
import kotlin.test.Test

class SalabimExampleTests {


    @Test
    fun `Bank_1_clerk should result in correct waiting line statistics`() {
        val env = configureEnvironment {
            add { Clerk() }
            add { ComponentQueue<Customer>("waiting line") }
        }.apply {
            CustomerGenerator()
        }.run(50.0)

        val waitingLine: ComponentQueue<Customer> = env.get()

        val expectedStats = JSONObject(
            """
            {
               "queue_length": {
                  "all": {
                     "duration": 50,
                     "min": 0,
                     "max": 1,
                     "mean": 0.121,
                     "standard_deviation": 0.33
                  },
                  "excl_zeros": {
                     "duration": 6.054825992605437,
                     "min": 1,
                     "max": 1,
                     "mean": 1,
                     "standard_deviation": 0
                  }
               },
               "name": "waiting line",
               "length_of_stay": {
                  "all": {
                     "entries": 5,
                     "mean": 21.709,
                     "standard_deviation": 15.885
                  },
                  "excl_zeros": {
                     "entries": 4,
                     "mean": 27.136,
                     "standard_deviation": 11.835
                  }
               },
               "type": "queue statistics"
            }
        """
        )

        //https://github.com/stleary/JSON-java/issues/573
        waitingLine.stats.toJson().toString(2) shouldBe expectedStats.toString(2)
//        waitingLine.stats.toJson().similar(expectedStats) shouldBe true
    }

    @Test
    fun `Bank3clerks_reneging should work as expected`() {
        val env = configureEnvironment {
            // register components needed for dependency injection
            add { ComponentQueue<org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.Customer>("waitingline") }
            add { State(false, "worktodo") }
            add { (0..2).map { org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.Clerk() } }
        }

        env.apply {
            // register other components to  be present when starting the simulation
            org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.CustomerGenerator()

            val waitingLine: ComponentQueue<org.github.holgerbrandl.kalasim.examples.kalasim.bank3clerks.reneging.Customer> =
                get()

            waitingLine.lengthOfStayMonitor.enabled = false
            run(1500.0)

            waitingLine.lengthOfStayMonitor.enabled = true
            run(500.0)
        }

        val waitingLine: ComponentQueue<Customer> = env.get()

        val expectedStats = JSONObject(
            """{"queue_length":{"all": {"duration": 2000, "min":0, "max": 3, "mean": 0.45,"standard_deviation": 0.672}, "excl_zeros": {"duration":702.6821809949577, "min": 1, "max": 3,"mean": 1.28, "standard_deviation": 0.474}},"name": "waitingline", "length_of_stay": {"all": {"entries": 193, "mean": 4.353, "standard_deviation":5.429}, "excl_zeros": {"entries": 125,"mean": 6.721, "standard_deviation": 5.44}},"type": "queue statistics"}"""
        )

        waitingLine.stats.toJson().toString(2) shouldBe expectedStats.toString(2)
    }


}