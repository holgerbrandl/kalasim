@file:OptIn(AmbiguousDuration::class)

package org.kalasim.examples

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kravis.*
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.json.JSONObject
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.kalasim.*
import org.kalasim.examples.bank.oneclerk.Clerk
import org.kalasim.examples.bank.oneclerk.Customer
import org.kalasim.examples.bank.reneging.CustomerGenerator
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.median
import org.kalasim.test.captureOutput
import org.koin.core.context.stopKoin
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


class SalabimExampleTests {


    @Test
    fun `Bank_1_clerk should result in correct waiting line statistics`() {
        val env = createSimulation {
            dependency { Clerk() }
            dependency { ComponentQueue<Customer>("waiting line") }

            org.kalasim.examples.bank.oneclerk.CustomerGenerator()
        }

        env.run(50.0)

        val waitingLine: ComponentQueue<Customer> = env.get()

        val expectedStats = JSONObject(
            """{
  "queue_length": {
    "all": {
      "duration": "50m",
      "min": 0,
      "max": 1,
      "mean": 0.121,
      "standard_deviation": 0.33
    },
    "excl_zeros": {
      "duration": "6m 3.289559556s",
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
      "median": 1.836,
      "mean": 1.211,
      "ninety_pct_quantile": 4.142,
      "standard_deviation": 1.836,
      "ninetyfive_pct_quantile": 4.142
    },
    "excl_zeros": {
      "entries": 2,
      "median": 1.576,
      "mean": 3.027,
      "ninety_pct_quantile": 4.142,
      "standard_deviation": 1.576,
      "ninetyfive_pct_quantile": 4.142
    }
  },
  "type": "QueueStatisticsSnapshot",
  "timestamp": "1970-01-01T00:50:00Z"
}"""
        )

        //https://github.com/stleary/JSON-java/issues/573
        waitingLine.statistics.toJson().toString(2) shouldBe expectedStats.toString(2)
//        waitingLine.stats.toJson().similar(expectedStats) shouldBe true
    }


    @Test
    fun `average waiting time should be constant in bank with 1 clerk`() {
        val runtimes = (20..120 step 10).map { it.days }

        val avgQueueMeans = runtimes.map { runtime ->
            runtime to createSimulation {
                dependency { Clerk(9.5.minutes) }
                dependency { ComponentQueue<Customer>("waiting line") }

                org.kalasim.examples.bank.oneclerk.CustomerGenerator()
                // ... with uniform(5,15).minutes

                run(runtime)
            }.run {
                val losStats =
                    get<ComponentQueue<Customer>>().statistics.lengthOfStayStats

//                get<ComponentQueue<Customer>>().lengthOfStayMonitor.display()
//                get<ComponentQueue<Customer>>().queueLengthMonitor.display()
//                get<ComponentQueue<Customer>>().lengthOfStayMonitor.display()

                stopKoin()

                losStats
            }
        }

        print(avgQueueMeans)

        @Suppress("ConstantConditionIf")
        if(false) {
            avgQueueMeans
                .plot(x = { it.first }, y = { it.second.median })
                .geomPoint()
                .geomLine()
                .show()

            Thread.sleep(100000)
        }

        // What is the expected mean here from a queuing theory perspective?
        // What is the mean waiting time with a uniform arrival between 5 and 15 minutes and a processing time of 10.minutes --> ChatGPT: since not exponential arrival no precise answer but roughly
        avgQueueMeans.map { it.second.median }.median() shouldBe 4.0.plusOrMinus(0.5)
    }

    @Test
    fun `Bank3clerks_reneging should work as expected`() {
        val env = createSimulation {
            // register components needed for dependency injection
            dependency { ComponentQueue<org.kalasim.examples.bank.reneging.Customer>("waitingline") }
            dependency { State(false, "worktodo") }
            dependency { (0..2).map { org.kalasim.examples.bank.reneging.Clerk() } }

            // register other components to  be present when starting the simulation
            CustomerGenerator()

            val waitingLine: ComponentQueue<org.kalasim.examples.bank.reneging.Customer> =
                get()

            waitingLine.lengthOfStayStatistics.enabled = false
            run(1500.0)

            waitingLine.lengthOfStayStatistics.enabled = true
//            waitingLine.lengthOfStayMonitor.reset()
            run(500.0)
        }

        val waitingLine: ComponentQueue<Customer> = env.get<ComponentQueue<Customer>>()

        val expectedStats = JSONObject(
            """{
  "queue_length": {
    "all": {
      "duration": "1d 9h 20m",
      "min": 0,
      "max": 3,
      "mean": 0.547,
      "standard_deviation": 0.72
    },
    "excl_zeros": {
      "duration": "13h 51m 38.317861985s",
      "min": 1,
      "max": 3,
      "mean": 1.316,
      "standard_deviation": 0.486
    }
  },
  "name": "waitingline",
  "length_of_stay": {
    "all": {
      "entries": 50,
      "median": 7.185,
      "mean": 10.497,
      "ninety_pct_quantile": 18.284,
      "standard_deviation": 7.185,
      "ninetyfive_pct_quantile": 21.968
    },
    "excl_zeros": {
      "entries": 45,
      "median": 6.602,
      "mean": 11.664,
      "ninety_pct_quantile": 19.167,
      "standard_deviation": 6.602,
      "ninetyfive_pct_quantile": 22.567
    }
  },
  "type": "QueueStatisticsSnapshot",
  "timestamp": "1970-01-02T09:20:00Z"
}"""
        )

        waitingLine.statistics.toJson().toString(2) shouldBe expectedStats.toString(2)

        listOf(1.0).toDoubleArray()
    }


    @Test
    fun `bank with resource clerks should result in correct statistics`() {
        val env = createSimulation {
            // same logic as in Bank3ClerksResources.kt
            dependency { Resource("clerks", capacity = 3) }

            ComponentGenerator(
                iat = UniformRealDistribution(
                    rg,
                    5.0,
                    15.0
                )
            ) {
                org.kalasim.examples.bank.resources.Customer(get())
            }
        }

        env.run(5000)

        val clerks = env.get<Resource>()
        clerks.apply {
            requesters.size shouldBeLessThan 10

            requesters.statistics.lengthStats.mean!! shouldBeLessThan 10.0
//            (requesters.stats.lengthStats as DescriptiveStatistics).getPercentile(.9) shouldBeLessThan  10.0

            claimers.statistics.lengthOfStayStats.mean shouldBe 30.0.plusOrMinus(0.1)
        }
    }


    @Test
    @Disabled
    fun `the atm queue should have known properties`() {
// TODO build tests using properties from https://en.wikipedia.org/wiki/M/M/1_queue
    }


    @Test
    fun `it should run all examples without exception`() {

        captureOutput {
            org.kalasim.examples.bank.oneclerk.main()
            org.kalasim.examples.bank.reneging.main()
            org.kalasim.examples.bank.resources.main()
            org.kalasim.examples.bank.state.main()
            org.kalasim.examples.bank.threeclerks.main()
        }
        // todo finish kts
        //  see https://kotlinexpertise.com/run-kotlin-scripts-from-kotlin-programs/
        //  see https://github.com/s1monw1/KtsRunner)
        // org.kalasim.examples.org.kalasim.examples.api.Cars..main()
    }
}
