package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.math.mean
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.kalasim.*
import org.kalasim.analysis.*
import org.kalasim.examples.bank.data.*
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.error.DefinitionOverrideException
import org.koin.core.error.NoDefinitionFoundException
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.test.fail
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class EnvTests {

    @Test
    fun `it should support more than one env`() {
        DependencyContext.stopKoin()

        class TestComponent(koin: Koin) : Component(koin = koin) {
            override fun process() = sequence {
                hold(2.minutes)
                println("my env is ${env.getKoin()}")
            }
        }

        val env1 = Environment(koin = koinApplication { }.koin)

        env1.apply {
            TestComponent(koin = getKoin())
            Resource(koin = env1.getKoin())
            State(false, koin = getKoin())
            ComponentQueue<Component>(koin = getKoin())

        }

        val env2 = Environment(koin = koinApplication { }.koin)

        TestComponent(koin = env2.getKoin())

        env1.run(10.hours)
        env2.run(10.hours)

        // make sure that the global context has not yet been started
//        shouldThrow<IllegalStateException> {
//            DependencyContext.get()
//        }
        // this assertion is no longer valid as `run` sets the context
    }

    @Test
    fun `it should run be possible to stop a simulation from an event-handler`() = createTestSimulation {
        var holdCounter = 0

        object : Component() {
            override fun repeatedProcess() = sequence {
                hold(1.minutes)
                holdCounter++

                if (holdCounter > 5) fail("simulation did not stop")
            }
        }

        addEventListener<RescheduledEvent> {
            if (it.time.toTickTime().value > 3 && it.type == ScheduledType.HOLD) {
                stopSimulation()
            }
        }

        run()
        holdCounter shouldBe 4
    }

    @Test
    fun `it should run be possible to run an old koin-context`() {

        // Note: make sure that we need DI during execution
        class TestResource(@Suppress("UNUSED_PARAMETER") resource: Resource) : Component()

        val env1 = Environment().apply {
            Component()

//            Resource()
            // Should we auto-declare when being in apply mode? --> No because how to deal with customerS!
            getKoin().declare(Resource())

            State(false)
            ComponentQueue<Component>()
            ComponentGenerator(iat = uniform().days) { TestResource(getKoin().get()) }

            run(1.minute)
        }

        val env2 = Environment().apply {
            Component()

//            Resource()
            getKoin().declare(Resource())

            State(false)

            ComponentQueue<Component>()
            ComponentGenerator(iat = uniform().days) { TestResource(getKoin().get()) }

            run(1.minute)
        }

        println("continuing env1...")
        class LateArriver(koin: Koin) : Component("late arriver", koin = koin)

        env1.addEventListener { println(it) }
//        shouldThrow<IllegalStateException> {
        env1.run(10.minutes)

        env1.apply {
            LateArriver(getKoin())
        }

        shouldThrow<NoDefinitionFoundException> {
            env2.get<LateArriver>()
        }

        println(env1.getKoin())
        println(env2.getKoin())
    }


    @Test
    fun `it should consume events asynchronously`() = createTestSimulation {
        ComponentGenerator(iat = constant(5).minutes) { Component("Car.${it}") }

        var consumed = false


        // add an asynchronous log consumer
        val asyncListener = addAsyncEventListener<EntityCreatedEvent> { event ->
            if (event.entity.name == "Car.1") {
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
        run(5.hours)

        sleep(4000)

        consumed shouldBe true

        // technically not needed here, but enabled for sake of test coverage
        asyncListener.stop()
    }

    @Test
    fun `it should allow to synchronize clock time`() {
        val timeBefore = System.currentTimeMillis()

        createSimulation {
            enableComponentLogger()

            ClockSync(500.milliseconds)

            run(10.minutes)
        }

       ( (System.currentTimeMillis() - timeBefore) / 1000.0 ) shouldBe (5.0 plusOrMinus 1.0)
    }

    @Test
    fun `it should log in strict order`() {
        class TestEvent(val label: String, time: SimTime) : Event(time)

        Environment().apply {
            val collect1 = collect<TestEvent>()

            val se = object : Component(){
                override fun process() =sequence<Component> {
                    log(TestEvent("1st", now))
                }
            }

            addEventListener<TestEvent> {
                if(it.label=="1st") {
                    se.log(TestEvent("2nd", now))
                }
            }

            val collect2 = collect<TestEvent>()

            run()

            collect1 shouldBe collect2
        }

    }


    @Test
    fun `it should log bus metrics`() {
        lateinit var sim: Environment

        val creationOutput = captureOutput {
            sim = EmergencyRoom(tickDurationUnit = DurationUnit.MINUTES, enableInternalMetrics = true).apply {
                // add mus metrics monitoring
                dependency { BusMetrics(timelineInterval = 10.minutes, walltimeInterval = 7.seconds) }

                // configure a down-takting
                ClockSync(500.milliseconds)
            }
        }

        creationOutput.stdout shouldBeDiff ""


        val runOutput = captureOutput {
            sim.run(30.minutes)
        }

        runOutput.stdout shouldBeDiff """
            INFO Component - BusMetrics: 13 events processed in last 10m
            INFO Component - BusMetrics: 1 events processed in last 10m
            INFO Component - BusMetrics: 7 events processed in last 10m
        """.trimIndent()

        val postOutput = captureOutput {
            sleep(10.seconds.inWholeMilliseconds)
        }

        postOutput.stdout shouldBeDiff """
            INFO Component - BusMetrics: 0.0 events processed on average per wall-time second
        """.trimIndent()


        val busMetrics = sim.get<BusMetrics>()

        busMetrics.stop()
        busMetrics.eventDistribution.statistics["RescheduledEvent"] shouldBe 10.0

        // nothing must be logged after stop
        val stoppedOutput = captureOutput {
            sleep(5.seconds.inWholeMilliseconds)
        }
        stoppedOutput.stdout shouldBeDiff ""
    }

    @Test
    fun `it should run a process at the end of the run time`() = createTestSimulation {
        // it's an important part of the spec if scheduled tasks at the end of the run() interval are executed or not
        // That's why we hardcode this behavior in a test

        var afterHour = false

        object : Component() {
            override fun process() = sequence {
                hold(60.minutes)
                afterHour = true
            }
        }

        run(1.hour, priority = Priority.LOW)

        afterHour shouldBe true
    }


    @Test
    fun `it should allow collecting events by type`() = createTestSimulation {
        ClockSync(500.milliseconds)

        val creations = collect<EntityCreatedEvent>()
        val cg = ComponentGenerator(exponential(1).minutes, total = 10) { Component() }

        // also check the filter works
        val creations2 = collect<EntityCreatedEvent> { it.time < startDate + 3.minutes }


        run(10.minutes)

        creations.size shouldBe (cg.total + 1) // +1 because of main

        creations2.size shouldBe 5 // because sim is deterministic here
    }

    @Test
    fun `it still support configuring dependencies before creating the simulation`() {
        class Car : Component() {
            override fun process() = sequence {
                hold(10.minutes)
            }
        }

        createSimulation {
            dependency { Car() }

            run(5.minutes)

            get<Car>().isScheduled shouldBe true
        }
    }

    @Test
    fun `it still should fail when last element in createSimulation is a dependency as this will fail internally otherwise`() {
        shouldThrow<IllegalArgumentException> {
            @Suppress("UNUSED_EXPRESSION")
            val sim = createSimulation {
                dependency("foo") { 1 }
                dependency("bar") { 2 }
            }

            sim.get<String>("foo")
            sim.run(1.days)
        }
    }


    @Test
    fun `it must allow registering the same dependency twice without qualifier`() {
        data class Foo(val flag: Boolean=true)
        shouldThrow<DefinitionOverrideException> {
            val sim = createSimulation {
                dependency{ Foo() }
                dependency{ Foo() }
                println()
            }

            sim.get<Foo>()
            sim.run(1.days)
        }
    }

    @Test
    fun `it should fail with exception if simulation is too slow `() {
        createSimulation {
            enableComponentLogger()

            object : Component() {
                var waitCounter = 1

                override fun repeatedProcess() = sequence {
                    hold(1.minutes)
                    // doe something insanely complex that takes 2seconds
                    sleep(waitCounter++ * 1000L)
                }
            }

            ClockSync(1.minutes, maxDelay = 1.seconds)

            shouldThrow<ClockOverloadException> {
                run(10.minutes)
            }.apply {
                timestamp shouldBeLessThan startDate + 10.minutes
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
                        hold(10.minutes)
                    }
            }

            run(until = null as Instant?)
            runtime shouldBe 10.minutes

            cc.size shouldBe 1
        }
    }

    @Test
    fun `it should allow to use custom duration units`() {
        val env = Environment(tickDurationUnit = DurationUnit.DAYS)

        env.run(4.days)

        env.runtime shouldBe 4.days
    }

    @Test
    fun `it should run until or duration until has reached`() {
        createSimulation {
            run(until = now + 10.minutes)
            runtime shouldBe 10.minutes
        }

        createSimulation {
            run(duration = 5.minutes)
            run(duration = 5.minutes)
            now shouldBe startDate + 10.minutes
        }
    }

    @Test
    fun `it should restore koin in before running sims in parallel`() {
        class QueueCustomer(mu: Double, val atm: Resource) : Component() {
            val ed = exponential(mu.minutes)

            override fun process() = sequence {
                request(atm) {
                    hold(ed())
                }
            }
        }

        class Queue(lambda: Double, val mu: Double) : Environment() {
            @Suppress("RedundantValueArgument")
            val atm = dependency { Resource("atm", capacity = 1) }

            init {
                ComponentGenerator(iat = exponential(lambda.minutes)) {
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
        atms.fastMap { it.run(100.minutes) }

        // to average over all configs does not make much sense conceptually, but allows to test for regressions
        val meanQLength = atms.map { it.get<Resource>().statistics.requesters.lengthStats.mean!! }.mean()
        meanQLength shouldBe (22.37 plusOrMinus 0.1)
    }


    @Test
    fun `it should stop a simulation`() = createTestSimulation {
        val events = enableEventLog()

        object : Component() {
            override fun process() = sequence {
                hold(10.minutes, "something is about to happen")
                stopSimulation()
                hold(10.minutes, "this ain't happening today")
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


    val testDataDir: Path
        get() = File("src/test/resources/stdout/EnvTests").toPath()

    @Test
    fun `it should log events as json`() {
        captureOutput {
            val er = EmergencyRoom(enableInternalMetrics = true)


            er.addEventListener {
//                println(GSON.toJson(it))
                println(it.toJson())
            }

            er.run(1.hours)
        }.stdout shouldBeDiff (testDataDir / "EnvTests_it_should_log_events_as_json.txt").toFile().readText()
    }


    // https://github.com/holgerbrandl/kalasim/issues/49
    @Disabled
    @Test
    fun `it should enforce typed durations`() {

        Environment().apply {

            object : Component() {
                override fun process() = sequence {
                    // try holding without duration unit --> should fail!
                    hold(10.minutes, "something is about to happen")
                    stopSimulation()
                }
            }

//            try {
            run() // try spinning the wheel until it should be stopped
//            }catch(tde: TypedDurationExpected)
        }
    }


    @Test
    fun `it should persist snapshots as json`() = testModel(EmergencyRoom()) {
        // does not work because some timelines are enabled upon creation
//        trackingPolicyFactory.enableAll()
        waitingLine.sizeTimeline.enabled = true

        run(10.days)

        doctors.first().snapshot.toJson().toIndentString() shouldBeDiff """
            {
              "requestedBy": [{
                "component": "room 2",
                "quantity": 1
              }],
              "claimedQuantity": 1,
              "creationTime": "1970-01-01T00:00:00Z",
              "now": "1970-01-11T00:00:00Z",
              "name": "Dr. Howe",
              "claimedBy": [{
                "first": "room 0",
                "second": null
              }],
              "capacity": 1
            }
        """.trimIndent()

        waitingLine.first().snapshot.toJson().toIndentString() shouldBeDiff """
        {
          "scheduledTime": null,
          "creationTime": "1970-01-06T17:15:42.348298110Z",
          "now": "1970-01-11T00:00:00Z",
          "name": "724 Heriberto Farrell",
          "claims": {},
          "requests": {},
          "status": "DATA"
        }
        """.trimIndent()

        waitingLine.sizeTimeline.snapshot.toJson().toIndentString() shouldBeDiff """
        {
          "duration": "10d",
          "min": 0,
          "max": 264,
          "mean": 74.096,
          "standard_deviation": 85.135
        }
        """.trimIndent()

        waitingLine.statistics.toJson().toIndentString() shouldBeDiff """
            {
              "size": {
                "all": {
                  "duration": "10d",
                  "min": 0,
                  "max": 264,
                  "mean": 74.096,
                  "standard_deviation": 85.135
                },
                "excl_zeros": {
                  "duration": "8d 1h 22m 25.304357505s",
                  "min": 1,
                  "max": 264,
                  "mean": 91.962,
                  "standard_deviation": 85.751
                }
              },
              "name": "ER Waiting Area Queue",
              "length_of_stay": {
                "all": {"entries": 0},
                "excl_zeros": {"entries": 0}
              },
              "type": "ComponentListStatistics",
              "timestamp": "1970-01-11T00:00:00Z"
            }
        """.trimIndent()
    }
}

class CustomKoinModuleTests {

    @Test
    // https://kotlinlang.slack.com/archives/C67HDJZ2N/p1671535899858929
    fun `avoid updating koin until 3_2_1 regression is fixed`() {
        data class Tester(val created: Long = System.nanoTime()){
            init {
                println("created")
            }
        }

        val myModule = module(createdAtStart = true) {
            single(createdAtStart = true) {
                Tester()
            }
        }

        val koinApplication = koinApplication {

        }
        val koin = koinApplication.koin
        koin.loadModules(modules = listOf(myModule))
        koinApplication.createEagerInstances()

        println("post-creation")


        val now = System.nanoTime()

        sleep(10)
        now shouldBeGreaterThan koin.get<Tester>().created
    }

    @Test
    fun `it should create components on simulation start`() {
        createSimulation {
            // register components needed for dependency injection
            dependency {
                println("foo")
                ComponentQueue<Customer>("waitingline")
            }
            dependency { CustomerGenerator(get()) }
            dependency { (1..3).map { Clerk() } }


            run(10.days)

            val waitingLine: ComponentQueue<Customer> = get()
            waitingLine.creationTime.epochSeconds.toInt() shouldBe 0

            println(waitingLine.statistics.toJson())
        }
    }

    @Test
    fun `it should allow to blacklist event types in event-log`() = createTestSimulation {

        class MyGoodEvent(time: SimTime) : Event(time)
        class MyBadEvent(time: SimTime) : Event(time)

        val eventLog = enableEventLog(blackList = listOf(MyBadEvent::class))

        object : Component(trackingConfig = ComponentTrackingConfig.NONE) {
            override fun process() = sequence<Component> {
                log(MyGoodEvent(now))
                log(MyBadEvent(now))
            }
        }

        run(10.minutes)

        eventLog.size shouldBe 2
        eventLog.last() shouldBe instanceOf(MyGoodEvent::class)
    }


    @OptIn(InternalKalasimApi::class)
    @Test
    fun `it should computing the scheduled time of a component`() = createTestSimulation {

        object : Component("foo", trackingConfig = ComponentTrackingConfig.NONE) {
            override fun process() = sequence<Component> {
                hold(4.hours)
            }
        }

        run(10.minutes)

        val match = computeQueueStatus().find { it.component.name == "foo" }!!

        match.remaining shouldBe (3.hours + 50.minutes)
    }
}