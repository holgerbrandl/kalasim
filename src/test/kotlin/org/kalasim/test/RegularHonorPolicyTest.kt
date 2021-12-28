package org.kalasim.test

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.analysis.ResourceEvent
import org.kalasim.analysis.ResourceEventType

class RegularHonorPolicyTest {

    class Customer(val arrivalTime: Int, val requestQuantity: Int) : Component() {
        val lawyers = get<Resource>()

        override fun process() = sequence {
            hold(arrivalTime)
            request(lawyers, quantity = requestQuantity)
            hold(1000)
            // usually we would use a request scope and relase the resource when done, but here we don't
        }
    }

    fun fruitStore(honorPolicy: RequestHonorPolicy): List<ResourceEvent> {

        val sim = createSimulation {
            val lawyers = Resource(capacity = 20, honorPolicy = honorPolicy)

            dependency { lawyers }

            Customer(1, 5)
            Customer(6, 6)
            Customer(15, 3)

            Customer(24, 1)  //release after request

            Customer(30, 3)

            Customer(37, 2)

            // refill the shelf after 5o ticks
            object : Component("release-manager") {
                override fun process() = sequence {
                    request(lawyers, quantity = lawyers.capacity) // law firm is fully booked
                    lawyers.claimed shouldBe 20
                    hold(20)

                    // incrementally refill banana shelf
                    repeat(5) {
                        release(lawyers, quantity = 4)
                        hold(4)
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


    fun `it should allow using a random policy`() {
        val takes = fruitStore(RequestHonorPolicy.RANDOM)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(3, 4, 2, 5, 1, 6)  // note: this was fixated after the first run
    }

    @Test
    fun `it should honor huge request eventually when using weighted SQF`() = createTestSimulation(false) {
        val lawyers = Resource(capacity = 10, honorPolicy = RequestHonorPolicy.WeightedFCFS(0.1))

        dependency { lawyers }

        val cg = ComponentGenerator(iat = exponential(6)) {
            object : Component() {
                override fun process() = sequence {
                    request(lawyers) {
                        hold(25)
                    }
                }
            }
        }

        // refill the shelf after 5o ticks
        object : Component("huge request") {
            override fun process() = sequence {
                // wait for requests to gather
                hold(200)

                request(lawyers, quantity = 10) {
                    stopSimulation()
                }
            }
        }

        val runFor = 1000
        run(runFor)

        println("num created ${cg.numGenerated}")
        now shouldBeLessThan TickTime(runFor)
    }
}