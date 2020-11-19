package org.github.holgerbrandl.kalasim.examples.kalasim

import io.kotest.matchers.shouldBe
import org.github.holgerbrandl.kalasim.ComponentQueue
import org.github.holgerbrandl.kalasim.add
import org.github.holgerbrandl.kalasim.configureEnvironment
import org.github.holgerbrandl.kalasim.examples.koiner.Clerk
import org.github.holgerbrandl.kalasim.examples.koiner.Customer
import org.github.holgerbrandl.kalasim.examples.koiner.CustomerGenerator
import org.json.JSONObject
import org.koin.core.get
import kotlin.test.Test

class SalabimExampleTests {


    @Test
    fun `Bank_1_clerk should result in correct waiting line statistics`(){
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
}