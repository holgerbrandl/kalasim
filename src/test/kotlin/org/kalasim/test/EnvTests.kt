package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import krangl.cumSum
import krangl.mean
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.junit.Test
import org.kalasim.*
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.error.NoBeanDefFoundException
import org.koin.dsl.koinApplication
import java.lang.Thread.sleep
import java.time.Duration

class EnvTests {

    @Test
    fun `it should support more than one env`() {
        DependencyContext.stopKoin()

        class TestComponent(koin: Koin) : Component(koin = koin) {
            override fun process() = sequence {
                hold(2)
                println("my env is ${env.getKoin()}")
            }
        }

        val env1 = Environment(koin = koinApplication { }.koin)

        env1.apply {

            val component = TestComponent(koin = getKoin())
            Resource(koin = env1.getKoin())
            State(false, koin = getKoin())
            ComponentQueue<Component>(koin = getKoin())

        }

        val env2 = Environment(koin = koinApplication { }.koin)

        val component2 = TestComponent(koin = env2.getKoin())

        env1.run(10)
        env2.run(10)

        // make sure that the global context has not yet been started
//        shouldThrow<IllegalStateException> {
//            DependencyContext.get()
//        }
        // this assertion is no longer valid as `run` sets the context
    }

    @Test
    fun `it should run be possible to run an old koin-context`() {

        // Note: make sure that we need DI during execution
        class TestResource(resource: Resource) : Component()

        val env1 = Environment().apply {
            Component()

//            Resource()
            // Should we auto-declare when being in apply mode? --> No because how to deal with customerS!
            _koin.declare(Resource())

            State(false)
            ComponentQueue<Component>()
            ComponentGenerator(iat = UniformRealDistribution()) { TestResource(getKoin().get()) }

            run(1)
        }

        println("setting up second simulation environment")
        val env2 = Environment().apply {
            Component()

//            Resource()
            _koin.declare(Resource())

            State(false)

            ComponentQueue<Component>()
            ComponentGenerator(iat = UniformRealDistribution()) { TestResource(getKoin().get()) }

            run(1)
        }

        println("continuing env1...")
        class LateArriver(koin: Koin) : Component("late arriver", koin = koin)

        env1.addEventListener { println(it) }
//        shouldThrow<IllegalStateException> {
        env1.run(10)
        env1.apply {
            LateArriver(getKoin())
        }

        shouldThrow<NoBeanDefFoundException> {
            env2.get<LateArriver>()
        }

        println(env1._koin)
        println(env2._koin)
    }


    @Test
    fun `it should consume events asynchronously`() = createTestSimulation {
        ComponentGenerator(iat = constant(1)) { Component("Car.${it}") }

        var consumed = false


        // add an asynchronous log consumer
        val asyncListener = addAsyncEventListener<EntityCreatedEvent> { event ->
            if(event.entity.name == "Car.1") {
                println("Consumed async!")
                consumed = true
            }
        }

        // Start another channel consumer
//        GlobalScope.launch {
//            asyncListener.eventChannel.receiveAsFlow()
//                .collect { consumed = true }
//        }

        // run the simulation
        run(5)

        sleep(4000)

        consumed shouldBe true

        // technically not needed here, but enabled for sake of test caverage
        asyncListener.stop()
    }

    @Test
    fun `it should allow to synchronize clock time`() {
        val timeBefore = System.currentTimeMillis()

        createSimulation(true) {
            ClockSync(Duration.ofMillis(500))

            run(10)
        }

        (System.currentTimeMillis() - timeBefore) / 1000.0 shouldBe 5.0.plusOrMinus(1.0)
    }


    @Test
    fun `it should allow collecting events by type`() = createTestSimulation(true) {
        ClockSync(Duration.ofMillis(500))

        val creations = collect<EntityCreatedEvent>()
        val cg = ComponentGenerator(exponential(1), total = 10) { Component() }

        run(10)

        creations.size shouldBe (cg.total + 1) // +1 because of main
    }

    @Test
    fun `it should fail with exception if simulation is too slow `() {
        createSimulation(true) {
            object : Component() {
                var waitCounter = 1
                override fun process() =
                    sequence {
                        while(true) {
                            hold(1)
                            // doe something insanely complex that takes 2seconds
                            sleep(waitCounter++ * 1000L)
                        }
                    }
            }

            ClockSync(Duration.ofSeconds(1), maxDelay = Duration.ofSeconds(3))

            shouldThrow<ClockOverloadException> {
                run(10)
            }.apply {
                simTime shouldBeLessThan 10.tt
            }
        }
    }

    @Test
    fun `it should run until event queue is empty`() {
        createSimulation {
            val cc = componentCollector()

            object : Component() {
                override fun process() =
                    sequence {
                        hold(10)
                    }
            }

            run(until = null)
            now shouldBe 10.tt

            cc.size shouldBe 1
        }
    }

    @Test
    fun `it should run until or duration until has reached`() {
        createSimulation {
            run(until = TickTime(10))
            now shouldBe 10.tt
        }

        createSimulation {
            run(duration = 5)
            run(duration = 5)
            now shouldBe 10.tt
        }
    }

    @Test
    fun `it should restore koin in before running sims in parallel`() {
        class QueueCustomer(mu: Double, val atm: Resource) : Component() {
            val ed = exponential(mu)

            override fun process() = sequence {
                request(atm) {
                    hold(ed.sample())
                }
            }
        }

        class Queue(lambda: Double, val mu: Double) : Environment() {
            val atm = dependency { Resource("atm", 1) }

            init {
                ComponentGenerator(iat = exponential(lambda)) {
                    QueueCustomer(mu, atm)
                }
            }
        }

        val lambdas = (1..20).map { 0.25 }.cumSum()
        val mus = (1..20).map { 0.25 }.cumSum()

        val atms = cartesianProduct(lambdas, mus).asIterable().map { (lambda, mu) ->
            Queue(lambda, mu)
        }

        // simulate in parallel
        atms.fastMap { it.run(100) }

        // to average over all configs does not make much sense conceptually, but allows to test for regressions
        val meanQLength = atms.map { it.get<Resource>().statistics.requesters.lengthStats.mean!! }.mean()
        meanQLength shouldBe (22.37 plusOrMinus 0.1)
    }


    @Test
    fun `it should stop a simulation`() = createTestSimulation {
        val events = eventLog()

        object : Component() {
            override fun process() = sequence {
                hold(10, "something is about to happen")
                stopSimulation()
                hold(10, "this ain't happening today")
            }
        }

        run() // try spinning the wheel until it should be stopped

        println("sim time after interruption is $now")
        events.size shouldBe 4

        run() // try spinning the wheel until the queue runs dry

        events.size shouldBe 5
        println("sim time after running dry is $now")
    }

    @Test
    fun `assert-modes should be correctly ordered`() {
        AssertMode.OFF shouldBeLessThan AssertMode.LIGHT
        AssertMode.LIGHT shouldBeLessThan AssertMode.FULL
    }

    @Test
    fun `it should log events as json`() {
        captureOutput {
            val er = EmergencyRoom(enableConsoleLogger = false)

//            er.apply { tickMetrics }

            er.addEventListener {
//                println(GSON.toJson(it))
                println(it.toJson())
            }

            er.run(1)
        }.stdout shouldBeDiff """
            {"receiver":"main","action":"running +1.00, scheduled for 1.00","details":"New state: scheduled","time":".00"}
            {"current":"TickMetrics.1","receiver":"TickMetrics.1","action":"hold +1.00, scheduled for 1.00","details":"New state: scheduled","time":".00"}
            {"current":"room 0","receiver":"room 0","action":"canceled","details":"New state: data","time":".00"}
            {"current":"room 1","receiver":"room 1","action":"canceled","details":"New state: data","time":".00"}
            {"current":"room 2","receiver":"room 2","action":"canceled","details":"New state: data","time":".00"}
            {"current":"room 3","receiver":"room 3","action":"canceled","details":"New state: data","time":".00"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.09, scheduled for .09","details":"New state: scheduled","time":".00"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Urgent","time":".09","entity":"State.1"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".09","entity":"State.2"}
            {"creator":"ComponentGenerator.1","time":".09","entity":"Patient.1"}
            {"current":"ComponentGenerator.1","receiver":"Patient.1","action":"activated, scheduled for .09","details":"New state: scheduled","time":".09"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.16, scheduled for .25","details":"New state: scheduled","time":".09"}
            {"current":"Patient.1","receiver":"Patient.1","action":"Ended","details":"New state: data","time":".09"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Resuscitation","time":".25","entity":"State.3"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".25","entity":"State.4"}
            {"creator":"ComponentGenerator.1","time":".25","entity":"Patient.2"}
            {"current":"ComponentGenerator.1","receiver":"Patient.2","action":"activated, scheduled for .25","details":"New state: scheduled","time":".25"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.23, scheduled for .48","details":"New state: scheduled","time":".25"}
            {"current":"Patient.2","receiver":"Patient.2","action":"Ended","details":"New state: data","time":".25"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Emergent","time":".48","entity":"State.5"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".48","entity":"State.6"}
            {"creator":"ComponentGenerator.1","time":".48","entity":"Patient.3"}
            {"current":"ComponentGenerator.1","receiver":"Patient.3","action":"activated, scheduled for .48","details":"New state: scheduled","time":".48"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.11, scheduled for .60","details":"New state: scheduled","time":".48"}
            {"current":"Patient.3","receiver":"Patient.3","action":"Ended","details":"New state: data","time":".48"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Emergent","time":".60","entity":"State.7"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".60","entity":"State.8"}
            {"creator":"ComponentGenerator.1","time":".60","entity":"Patient.4"}
            {"current":"ComponentGenerator.1","receiver":"Patient.4","action":"activated, scheduled for .60","details":"New state: scheduled","time":".60"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.10, scheduled for .70","details":"New state: scheduled","time":".60"}
            {"current":"Patient.4","receiver":"Patient.4","action":"Ended","details":"New state: data","time":".60"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Urgent","time":".70","entity":"State.9"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".70","entity":"State.10"}
            {"creator":"ComponentGenerator.1","time":".70","entity":"Patient.5"}
            {"current":"ComponentGenerator.1","receiver":"Patient.5","action":"activated, scheduled for .70","details":"New state: scheduled","time":".70"}
            {"current":"ComponentGenerator.1","receiver":"room 0","action":"Activating process=process, scheduled for .70","details":"New state: scheduled","time":".70"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.04, scheduled for .74","details":"New state: scheduled","time":".70"}
            {"current":"Patient.5","receiver":"Patient.5","action":"Ended","details":"New state: data","time":".70"}
            {"current":"room 0","receiver":"room 0","action":"hold +.13  preparing room room 0 for Dislocations, scheduled for .83","details":"New state: scheduled","time":".70"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Resuscitation","time":".74","entity":"State.11"}
            {"creator":"ComponentGenerator.1","details":"Initial value: Waiting","time":".74","entity":"State.12"}
            {"creator":"ComponentGenerator.1","time":".74","entity":"Patient.6"}
            {"current":"ComponentGenerator.1","receiver":"Patient.6","action":"activated, scheduled for .74","details":"New state: scheduled","time":".74"}
            {"current":"ComponentGenerator.1","receiver":"ComponentGenerator.1","action":"hold +.35, scheduled for 1.09","details":"New state: scheduled","time":".74"}
            {"current":"Patient.6","receiver":"Patient.6","action":"Ended","details":"New state: data","time":".74"}
            {"requester":"room 0","requesters":1,"current":"room 0","amount":1,"resource":"Dr 0","occupancy":0,"claimed":0,"time":".83","type":"REQUESTED","claimers":0,"request_id":-6457245767123267605,"capacity":1}
            {"requester":"room 0","requesters":1,"current":"room 0","amount":1,"resource":"Dr 1","occupancy":0,"claimed":0,"time":".83","type":"REQUESTED","claimers":0,"request_id":-8473867053398002461,"capacity":1}
            {"requester":"room 0","requesters":1,"current":"room 0","amount":1,"resource":"Dr 2","occupancy":0,"claimed":0,"time":".83","type":"REQUESTED","claimers":0,"request_id":-3604026565907225053,"capacity":1}
            {"requester":"room 0","requesters":1,"current":"room 0","amount":1,"resource":"Dr 3","occupancy":0,"claimed":0,"time":".83","type":"REQUESTED","claimers":0,"request_id":-7522904868940355451,"capacity":1}
            {"requester":"room 0","requesters":1,"current":"room 0","amount":1,"resource":"Dr 0","occupancy":1,"claimed":1,"time":".83","type":"CLAIMED","claimers":0,"request_id":-6457245767123267605,"capacity":1}
            {"current":"room 0","receiver":"room 0","action":"Request honored by Dr 0, scheduled for .83","details":"New state: scheduled","time":".83"}
            {"current":"room 0","receiver":"room 0","action":"hold +.59  Surgery of patient Patient(type=Dislocations, severity=State.9[Urgent], patientStatus=State.10[InSurgery]) in room room 0 by doctor RequestScopeContext(resource=Dr 0, requestingSince=.83), scheduled for 1.42","details":"New state: scheduled","time":".83"}
            """.trimIndent()
    }


    @Test
    fun `it should persist snapshots as json`() {
        val er = EmergencyRoom(enableConsoleLogger = false)
        er.run(10)

        er.doctors.first().snapshot.toJson().toIndentString() shouldBeDiff """
            {
              "requestedBy": [],
              "claimedQuantity": 1,
              "creationTime": 0,
              "now": 10,
              "name": "Dr 0",
              "claimedBy": [{
                "first": "room 2",
                "second": null
              }],
              "capacity": 1
            }
        """.trimIndent()

        er.waitingLine.first().snapshot.toJson().toIndentString() shouldBeDiff """
            {
              "scheduledTime": null,
              "creationTime": 9.71,
              "now": 10,
              "name": "Patient.58",
              "claims": {},
              "requests": {},
              "status": "DATA"
            }
        """.trimIndent()

        er.waitingLine.sizeTimeline.snapshot.toJson().toIndentString() shouldBeDiff """
            {
              "duration": 10,
              "min": 0,
              "max": 2,
              "mean": 0.076,
              "standard_deviation": 0.375
            }
        """.trimIndent()

        er.waitingLine.statistics.toJson().toIndentString() shouldBeDiff """
            {
              "size": {
                "all": {
                  "duration": 10,
                  "min": 0,
                  "max": 2,
                  "mean": 0.076,
                  "standard_deviation": 0.375
                },
                "excl_zeros": {
                  "duration": 0.4761825391449257,
                  "min": 1,
                  "max": 2,
                  "mean": 1.595
                }
              },
              "name": "ER Waiting Area Queue",
              "length_of_stay": {
                "all": {
                  "entries": 16,
                  "median": 0.079,
                  "mean": 0.029,
                  "ninety_pct_quantile": 0.142,
                  "standard_deviation": 0.079,
                  "ninetyfive_pct_quantile": 0.31
                },
                "excl_zeros": {
                  "entries": 4,
                  "median": 0.131,
                  "mean": 0.117,
                  "ninety_pct_quantile": 0.31,
                  "standard_deviation": 0.131,
                  "ninetyfive_pct_quantile": 0.31
                }
              },
              "type": "ComponentListStatistics",
              "timestamp": 10
            }
        """.trimIndent()
    }
}