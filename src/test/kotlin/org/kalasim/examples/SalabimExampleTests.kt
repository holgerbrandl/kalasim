package org.kalasim.examples

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kravis.*
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.descriptive.rank.Median
import org.json.JSONObject
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*
import org.kalasim.examples.bank.oneclerk.Clerk
import org.kalasim.examples.bank.oneclerk.Customer
import org.kalasim.examples.bank.reneging.CustomerGenerator
import org.kalasim.misc.median
import org.kalasim.test.captureOutput
import org.koin.core.context.stopKoin


internal val DescriptiveStatistics.median: Double
    get() = Median().evaluate(values)

class SalabimExampleTests {


    @Test
    fun `Bank_1_clerk should result in correct waiting line statistics`() {
        val env = configureEnvironment {
            add { Clerk() }
            add { ComponentQueue<Customer>("waiting line") }
        }.apply {
            org.kalasim.examples.bank.oneclerk.CustomerGenerator()
        }

        env.run(50.0)

        val waitingLine: ComponentQueue<Customer> = env.get()

        val expectedStats = JSONObject(
            """{
              "queue_length": {
                "all": {
                  "duration": 50,
                  "min": 0,
                  "max": 1,
                  "mean": 0.119,
                  "standard_deviation": 0.327
                },
                "excl_zeros": {
                  "duration": 5.965374252531483,
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
                  "median": 1.81,
                  "mean": 1.193,
                  "ninety_pct_quantile": 4.083,
                  "standard_deviation": 1.81,
                  "ninetyfive_pct_quantile": 4.083
                },
                "excl_zeros": {
                  "entries": 2,
                  "median": 1.557,
                  "mean": 2.983,
                  "ninety_pct_quantile": 4.083,
                  "standard_deviation": 1.557,
                  "ninetyfive_pct_quantile": 4.083
                }
              },
              "type": "QueueStatistics",
              "timestamp": 50
            }"""
        )

        //https://github.com/stleary/JSON-java/issues/573
        waitingLine.statistics.toJson().toString(2) shouldBe expectedStats.toString(2)
//        waitingLine.stats.toJson().similar(expectedStats) shouldBe true
    }


    @Test
    fun `average waiting should be constant in bank with 1 clerk`() {
        // todo what is the expected mean here from a queuing theory perspective?

        val avgQueueMeans = (20..120 step 10).map { 1000.0 * it }.map { runtime ->
//        val avgQueueMeans = listOf(200).map { 1000.0 * it }.map { runtime ->
//        val avgQueueMeans = listOf(100000).map { runtime ->
            runtime to declareDependencies {
                add { Clerk() }
                add { ComponentQueue<Customer>("waiting line") }
            }.createSimulation {
                org.kalasim.examples.bank.oneclerk.CustomerGenerator()
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

        if(false) {
            avgQueueMeans
                .plot(x = { it.first }, y = { (it.second.ss as DescriptiveStatistics).median })
                .geomPoint()
                .geomLine()
                .show()
            Thread.sleep(100000)
        }

        avgQueueMeans.map { (it.second.ss as DescriptiveStatistics).median }.median() shouldBe 27.0.plusOrMinus(1.0)
    }

    @Test
    fun `Bank3clerks_reneging should work as expected`() {
        val env = declareDependencies {
            // register components needed for dependency injection
            add { ComponentQueue<org.kalasim.examples.bank.reneging.Customer>("waitingline") }
            add { State(false, "worktodo") }
            add { (0..2).map { org.kalasim.examples.bank.reneging.Clerk() } }
        }.createSimulation {

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

        val waitingLine: ComponentQueue<Customer> = env.get()

        val expectedStats = JSONObject(
            """{
              "queue_length": {
                "all": {
                  "duration": 2000,
                  "min": 0,
                  "max": 3,
                  "mean": 0.547,
                  "standard_deviation": 0.72
                },
                "excl_zeros": {
                  "duration": 831.6386310335986,
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
              "type": "QueueStatistics",
              "timestamp": 2000
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
    @Ignore
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
