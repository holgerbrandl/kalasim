package org.kalasim.examples.api

import org.kalasim.*


class Customer(val arrivalTime: Int, val requestQuantity: Int) : Component() {
    val bananas = get<DepletableResource>()

    override fun process() = sequence {
        hold(arrivalTime)
        request(bananas, quantity = requestQuantity)
    }
}

fun fruitStore(honorPolicy: RequestHonorPolicy): List<ResourceEvent> {

    val sim = createSimulation {
        val bananas = DepletableResource(capacity = 100, initialLevel = 0, honorPolicy = honorPolicy)

        dependency { bananas }

        Customer(1, 4)
        Customer(6, 5)
        Customer(15, 5)
        Customer(24, 1)
        Customer(40, 3)

        // refill the shelf after 5o ticks
        object : Component() {
            override fun process() = sequence {
                hold(50)

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
        return resourceEvents
    }
}


// explore different resource policies
val events = fruitStore(RequestHonorPolicy.RelaxedFCFS)
//val events = fruitStore(RequestHonorPolicy.StrictFCFS)
//val events = fruitStore(RequestHonorPolicy.SQF).map{ it.requester}
//val events = fruitStore(RequestHonorPolicy.WeightedFCFS(0.4)).map{ it.requester}

//events.forEach { println(it) }

val takes = events.filter { it.type == ResourceEventType.TAKE }
takes.forEach { println(it.requester.name) }

println("")