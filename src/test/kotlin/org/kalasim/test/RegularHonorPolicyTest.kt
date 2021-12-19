package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.misc.printThis

class RegularHonorPolicyTest {

    class Customer(val arrivalTime: Int, val requestQuantity: Int) : Component() {
        val lawyers = get<Resource>()

        override fun process() = sequence {
            hold(arrivalTime)
            request(lawyers, quantity = requestQuantity)
            // usually we would use a request scope and relase the resource when done, but here we don't
        }
    }

    fun fruitStore(honorPolicy: RequestHonorPolicy): List<ResourceEvent> {

        val sim = createSimulation {
            val lawyers = Resource(capacity = 20, honorPolicy = honorPolicy)

            dependency { lawyers }

            Customer(1, 4)
            Customer(6, 5)
            Customer(15, 3)
            Customer(24, 1)
            Customer(40, 3)
            Customer(44, 2)

            // refill the shelf after 5o ticks
            object : Component() {
                override fun process() = sequence {
                    request(lawyers, quantity = lawyers.capacity) // law firm is fully booked
                    hold(20)

                    // incrementally refill banana shelf
                    repeat(5) {
                        release(lawyers, quantity = 4)
                        hold(10)
                    }
                }
            }
        }

        with(sim) {
            val resourceEvents = collect<ResourceEvent>()
            run()

            println("remaining level after running for ${now}: ${(resourceEvents.first().resource as DepletableResource).level}")
            return resourceEvents.filter { it.type == ResourceEventType.TAKE }
        }
    }

    @Test
    fun `it should allow using a relaxed FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.RelaxedFCFS)
        takes.map { "${it.requester} (${it.time})" }.joinToString(", ").printThis()
        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(4, 5, 6, 1, 2, 3)
    }

    @Test
    fun `it should allow using a strict FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.StrictFCFS)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `it should allow using a SQF`() {
        val takes = fruitStore(RequestHonorPolicy.SQF)

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `it should allow using a weighted SQF`() {
        val takes = fruitStore(RequestHonorPolicy.WeightedSQF(0.4))

        TODO() // complete test defintion

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(4, 5, 1, 2, 3)
    }
}