package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.*
import org.kalasim.misc.printThis

class DepletableHonorPolicyTest {

    class Customer(val arrivalTime: Int, val requestQuantity: Int) : Component() {
        val bananas = get<DepletableResource>()

        override fun process() = sequence {
            hold(arrivalTime)
            request(bananas, quantity = requestQuantity)
        }
    }

    fun fruitStore(honorPolicy: RequestHonorPolicy): List<ResourceEvent> {

        val sim = createSimulation {
            val bananas = DepletableResource(capacity = 20, initialLevel = 0, honorPolicy = honorPolicy)

            dependency { bananas }

            Customer(1, 4)
            Customer(6, 5)
            Customer(15, 3)
            Customer(24, 1)
            Customer(40, 3)
            Customer(44, 2)

            // refill the shelf after 5o ticks
            object : Component() {
                override fun process() = sequence {
                    hold(20)

                    // incrementally refill banana shelf
                    repeat(6) {
                        put(bananas, quantity = 4)
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

//        takes.map { "${it.requester} (${it.time})" }.joinToString(", ").printThis()

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(1, 3, 4, 5, 2, 6) // note: this was inferred from the test and not worked out on paper
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
        } shouldBe listOf(3, 4, 1, 5, 6, 2) // note: this was inferred from the test and not worked out on paper
    }

    @Test
    fun `it should allow using a weighted FCFS`() {
        val takes = fruitStore(RequestHonorPolicy.WeightedFCFS(0.4))

        takes.map {
            it.requester.name.replace("Customer.", "").toInt()
        } shouldBe listOf(1, 4, 3, 5, 2, 6)  // note: this was inferred from the test and not worked out on paper
    }
}