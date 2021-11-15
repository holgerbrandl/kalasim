package org.kalasim.test

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.junit.Assert
import org.junit.Test
import org.kalasim.*
import org.kalasim.ResourceSelectionPolicy.*

class ResourceTests {

    @Test
    fun `preemptive resources should bump claims`() {
        // see version 19.0.9  2019-10-08 in salabim change log for code snippets

        /**
        if the component has to start all over again (hold(1)) if it is bumped:
        def process(self):
        prio = sim.Pdf((1,2,3), 1)
        while True:
        yield self.request((preemptive_resource, 1, prio)
        yield self.hold(1)
        if self.isclaiming(preemptive_resource):
        break
        self.release(preemptive_resource)
         */


        createSimulation {
            val resource = Resource(preemptive = true)

            val prioPDF = EnumeratedDistribution(listOf(1, 2, 3).map { it to 1.0 / 3.0 }.asCMPairList())

            object : Component() {

                override fun process() = sequence {
                    while (true) {
                        request(resource withQuantity 1 andPriority Priority(prioPDF.sample()))
                        hold(1)
                        if (!isClaiming(resource)) {
                            break
                        } else {
                            release(resource)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `it should release resourced when terminating`() = createTestSimulation(true) {

        // see bank office docs: By design resources that were claimed are automatically
        // released when a process terminates

        val resource = Resource()

        object : Component("foo") {

            override fun process() = sequence {
                request(resource)
                hold(1)
                log("finished process, terminating...")
            }
        }

        run(10)

        resource.claimers.size shouldBe 0
    }

    @Test
    fun `preemptive resources should bump components depending on priority`() = createTestSimulation(true) {

        // see bank office docs: By design resources that were claimed are automatically
        // released when a process terminates

        val resource = Resource("repair", preemptive = true, capacity = 2)

        class BumpComponent(
            name: String,
            val preRequestHold: Int,
            val postRequestHold: Int,
            val requestPriority: Int? = null,
            val failOnBump: Boolean = false
        ) : Component(name) {

            override fun process() = sequence {
                hold(preRequestHold)

                if (requestPriority != null) {
                    request(resource withPriority requestPriority)
                } else {
                    request(resource)
                }

                hold(postRequestHold)

                if (isBumped(resource)) {
                    log("got bumped from $resource")
                    if (failOnBump) Assert.fail()
                    return@sequence
                }

                log("finished process, terminating...")
            }
        }

        val tpTool = BumpComponent("top-prio tool", 0, 100, requestPriority = 100)
        val lpTool = BumpComponent("low-prio tool", 0, 5)
        val mpTool = BumpComponent("mid-prio tool", 10, 5)
        val hpTool = BumpComponent("high-prio tool", 11, 5, requestPriority = 3)
        val hpTool2 = BumpComponent("high-prio tool 2", 12, 5, requestPriority = 3)

        run(10)

        // it should have auto-release the resource by now
        resource.claimers.size shouldBe 1
        lpTool.isRequesting shouldBe false
        lpTool.isData shouldBe true
        lpTool.requests.shouldBeEmpty()

        run(5)

        // at this point hp-tool should have bumped mpTool
        mpTool.isBumped(resource) shouldBe true
        mpTool.isData shouldBe true

        hpTool.isScheduled shouldBe true
        hpTool2.isRequesting shouldBe true

        resource.requesters.contains(hpTool2) shouldBe true
        resource.claimers.contains(hpTool) shouldBe true


        run(5)
        hpTool.isData shouldBe true
        hpTool2.isScheduled shouldBe true

        resource.requesters.size shouldBe 0
        resource.claimers.contains(hpTool2) shouldBe true
        resource.claimers.contains(tpTool) shouldBe true
        resource.claimers.size shouldBe 2
    }

    @Test
    fun `null prio requests should not bump each other`() = createTestSimulation(true) {
        val r = Resource(preemptive = true)

        class BumpComponent : Component() {

            override fun process() = sequence {
                request(r)
                hold(5)
                log("finished process, terminating...")
            }
        }

        val bc1 = BumpComponent()
        val bc2 = BumpComponent()

        run(3)

        bc1.isScheduled shouldBe true
        bc2.isRequesting shouldBe true
    }

    @Test
    fun `it should not bump regular non-preemptive resources`() = createTestSimulation(true) {
        val r = Resource()

        class BumpComponent : Component() {

            override fun process() = sequence {
                request(r)
                hold(5)
                log("finished process, terminating...")
            }
        }

        val bc1 = BumpComponent()
        val bc2 = BumpComponent()

        run(8)

        bc1.isData shouldBe true
        bc2.isScheduled shouldBe true
    }

    @Test
    fun `it should reevaluate requests upon capacity changes`() {

        class Customer(val clerk: Resource) : Component() {

            override fun process() = sequence {
                hold(ticks = 5.0)

                request(clerk)
                hold(ticks = 2.0, priority = IMPORTANT)
                release(clerk)

                passivate()
                hold(ticks = 5.0)
            }
        }

        createSimulation {
            val clerk = Resource(capacity = 0)

            val customer = Customer(clerk)

            run(8)
            clerk.capacity = 1.0

            run(10)
            customer.activate()

//            sequence<Component>{
//                with(customer){
//                    hold(3)
//                }
//            }.toList()

            run(10)

            // how long was the component in passive state
            customer.statusMonitor.printHistogram()
//                println(customer.statusMonitor[ComponentState.PASSIVE])

            customer.statusMonitor.total(ComponentState.PASSIVE) shouldBe 8.0
        }
    }

    @Test
    fun `it should auto-release resources in builder`() {

        createSimulation(true) {
            val r = Resource()

            object : Component() {
                override fun process() = sequence {
                    hold(2)

                    request(ResourceRequest(r)) {
                        hold(1)
                    }
                }
            }

            println(toString())

            run(5)

            println(toString())

            r.claimers.isEmpty() shouldBe true
            r.requesters.isEmpty() shouldBe true
        }
    }

    @Test
    fun `it should report correct resource in honor block when using oneOf mode`() = createTestSimulation {
        val r1 = Resource(capacity = 3)
        val r2 = Resource(capacity = 3)
        val r3 = Resource(capacity = 3)

        var honorBlockReached = false
        object : Component() {
            override fun process() = sequence {
                request(r2 withQuantity 2)

                request(r1) {
                    request(r2) {
                        request(r2, r3, oneOf = true) { honored ->
                            honored shouldBe r3
                            println("honor block")
                            honorBlockReached = true
                        }
                    }
                }
            }
        }

        run(1)

        honorBlockReached shouldBe true
    }

    @Test
    fun `it should correctly set failed after timeout`() {

        createSimulation(true) {
            val r = Resource(capacity = 1)
            val dr = Resource(capacity = 1)

            val c = object : Component() {
                override fun process() = sequence {
                    request(r)
                    request(dr)

                    //now try again but since both resources are busy/depleted it should fail
                    // irrespective of the delay or time
                    request(r, failDelay = 0)
                    failed shouldBe true
                    request(dr, failDelay = 0)
                    failed shouldBe true

                    request(r, failDelay = 1)
                    failed shouldBe true
                    request(dr, failDelay = 1)
                    failed shouldBe true

                    request(r, failAt = now + 1)
                    failed shouldBe true
                    request(dr, failAt = now + 1)
                    failed shouldBe true
                }
            }

            run(5)

            println(toString())

            r.claimers.isEmpty() shouldBe true
            r.requesters.isEmpty() shouldBe true

            dr.claimers.isEmpty() shouldBe true
            dr.requesters.isEmpty() shouldBe true

            c.requests.shouldBeEmpty()
            c.componentState shouldBe ComponentState.DATA
        }
    }

    @Test
    fun `it should correctly handle oneOf requests`() = createTestSimulation {
        class DoctorMeier : Resource()
        class DoctorSchreier : Resource()

        val doctors: List<Resource> = listOf(DoctorMeier(), DoctorSchreier())

        val patient = object : Component() {
            override fun process() = sequence {
                request(doctors, oneOf = true) {
                    hold(1)
                }
            }
        }

        run(10)

        doctors.forEach { dr ->
            dr.requesters.q.shouldBeEmpty()
            dr.claimers.q.shouldBeEmpty()
        }

        patient.componentState shouldBe ComponentState.DATA
    }

}

class ResourceSelectionTests {
    @Test
    fun `it should allow to select with FIRST_AVAILBLE`() = createTestSimulation(true) {
        val resources = List(3) { Resource().apply { capacity = 0.0 } }

        object : Component() {
            override fun process() = sequence {
                val r = selectResource(resources, policy = FIRST_AVAILABLE)
                r shouldBe resources[1]
            }
        }

        run(3)
        resources[1].capacity = 3.0
        run(3)
    }

    @Test
    fun `it should allow to select with SHORTEST_QUEUE`() = createTestSimulation(true) {
        val resources = List(3) { Resource() }

        class ResourceConsumer(val resource: Resource) : Component() {
            override fun process() = sequence {
                request(resource) { hold(10) }
            }
        }

        repeat(10) { ResourceConsumer(resources[0]) }
        repeat(3) { ResourceConsumer(resources[1]) }
        repeat(20) { ResourceConsumer(resources[2]) }

        object : Component() {
            override fun process() = sequence {
                val r = selectResource(resources, policy = SHORTEST_QUEUE)
                r shouldBe resources[1]
            }
        }

        run(3)
        resources[1].capacity = 3.0
        run(3)
    }

    @Test
    fun `it should allow to select with ROUND_ROBIN`() = createTestSimulation(true) {
        val resources = List(3) { Resource() }

        val c = object : Component() {

            val obtainedResources = mutableListOf<Resource>()

            override fun process() = sequence {
                repeat(9) {
                    val r = selectResource(resources, policy = ROUND_ROBIN)
                    request(r) {
                        hold(1)
                    }

                    obtainedResources.add(r)
                }
            }
        }

        run(20)

        c.obtainedResources shouldBe (resources + resources + resources)
    }
}



