package org.kalasim.test

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.AmbiguousDurationComponent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class RegularHonorPolicyTest {

    class Customer(val arrivalTime: Duration, val requestQuantity: Int) : Component() {
        val bananas = get<Resource>()

        override fun process() = sequence {
            hold(arrivalTime)
            request(bananas, quantity = requestQuantity)
            hold(1000.days)// todo why that?
            // usually we would use a request scope and release the resource when done, but here we don't
        }
    }

    fun fruitStore(honorPolicy: RequestHonorPolicy): List<ResourceEvent> {

        val sim = createSimulation {
            val lawyers = Resource(capacity = 20, honorPolicy = honorPolicy)

            dependency { lawyers }

            Customer(1.minutes, 5)
            Customer(6.minutes, 6)
            Customer(15.minutes, 3)

            Customer(24.minutes, 1)  //release after request

            Customer(30.minutes, 3)

            Customer(37.minutes, 2)

            // refill the shelf after 5o ticks
            object : Component("release-manager") {
                override fun process() = sequence {
                    request(lawyers, quantity = lawyers.capacity) // banana shelf is fully stocked

                    lawyers.claimed shouldBe 20

                    hold(20.minutes)

                    // incrementally refill banana shelf
                    repeat(5) {
                        release(lawyers, quantity = 4)
                        hold(4.minutes)
                    }

                    stopSimulation() // because otherwise customer will release when being terminated
                }
            }
        }

        with(sim) {
            val resourceEvents = collect<ResourceEvent>()
            run()

            println("remaining level after running for ${now}: ${(resourceEvents.first().resource).claimed}")
            return resourceEvents.filter { it.type == ResourceEventType.CLAIMED }.drop(1) // drop the initial request
        }
    }

    @Test
    fun `it should allow using a relaxed FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.RelaxedFCFS)

//        takes.map { "${it.requester} (${it.time})" }.joinToString(", ").printThis()
//        takes.forEach{ println(it)}

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(3, 4, 1, 5, 2, 6)
    }

    @Test
    fun `it should allow using a strict FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.StrictFCFS)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `it should allow using a SQF`() {
        val takes = fruitStore(RequestHonorPolicy.SQF)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(3, 4, 1, 5, 2, 6) // that's unfortunately the same as for R-FCFS in this example
    }

    @Test
    fun `it should allow using a weighted FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.WeightedFCFS(0.01))

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(3, 4, 1, 5, 2, 6)  // note: this was inferred from the test and not worked out on paper
    }  @Test


    // NOTE: This test will inevetiably fail, if more numbers were samples in the test-sim than when fixating the result
    fun `it should allow using a random policy`() {
        val takes = fruitStore(RequestHonorPolicy.RANDOM)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(3, 4, 1, 5, 2, 6)  // note: this was fixated after the first run
    }

    @OptIn(AmbiguousDuration::class, AmbiguousDurationComponent::class)
    @Test
    fun `it should honor huge request eventually when using weighted SQF`() = createTestSimulation(enableComponentLogger = false) {
        val lawyers = Resource(capacity = 10, honorPolicy = RequestHonorPolicy.WeightedFCFS(0.1))

        dependency { lawyers }

        val cg = ComponentGenerator(iat = exponential(6)) {
            object : TickedComponent() {
                override fun process() = sequence {
                    request(lawyers) {
                        hold(25)
                    }
                }
            }
        }

        // refill the shelf after 5o ticks
        object : TickedComponent("huge request") {
            override fun process() = sequence {
                // wait for requests to gather
                hold(200)

                request(lawyers, quantity = 10) {
                    stopSimulation()
                }
            }
        }

        val runFor = 1000.minutes
        run(runFor)

        println("num created ${cg.numGenerated}")
        now shouldBeLessThan startDate + runFor
    }
}