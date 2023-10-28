package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import junit.framework.Assert.fail
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import krangl.cumSum
import krangl.mean
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.junit.Ignore
import org.junit.Test
import org.kalasim.*
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.analysis.RescheduledEvent
import org.kalasim.examples.bank.data.Clerk
import org.kalasim.examples.bank.data.Customer
import org.kalasim.examples.bank.data.CustomerGenerator
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.error.NoBeanDefFoundException
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import java.lang.Thread.sleep
import kotlin.io.path.div
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun main() {
    val now = Clock.System.now()
    val instant = now + 5.minutes
    println(instant)
}

@OptIn(AmbiguousDurationComponent::class)
class EnvTests {

    @Test
    fun `it should support more than one env`() {
        DependencyContext.stopKoin()

        class TestComponent(koin: Koin) : TickedComponent(koin = koin) {
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
    fun `it should run be possible to stop a simulation from an event-handler`() = createTestSimulation {
        var holdCounter = 0

        object : TickedComponent() {
            override fun repeatedProcess() = sequence {
                hold(1)
                holdCounter++

                if(holdCounter > 5) fail("simulation did not stop")
            }
        }

        addEventListener<RescheduledEvent> {
            if(it.time.toTickTime().value > 3 && it.type == ScheduledType.HOLD) {
                stopSimulation()
            }
        }

        run()
        holdCounter shouldBe 4
    }

    @Test
    fun `it should run be possible to run an old koin-context`() {

        // Note: make sure that we need DI during execution
        class TestResource(resource: Resource) : Component()

        val env1 = Environment().apply {
            Component()

//            Resource()
            // Should we auto-declare when being in apply mode? --> No because how to deal with customerS!
            getKoin().declare(Resource())

            State(false)
            ComponentQueue<Component>()
            ComponentGenerator(iat = UniformRealDistribution()) { TestResource(getKoin().get()) }

            run(1)
        }

        println("setting up second simulation environment")
        val env2 = Environment().apply {
            Component()

//            Resource()
            getKoin().declare(Resource())

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

        println(env1.getKoin())
        println(env2.getKoin())
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

        createSimulation {
            enableComponentLogger()

            ClockSync(500.milliseconds)

            run(10)
        }

        (System.currentTimeMillis() - timeBefore) / 1000.0 shouldBe 5.0.plusOrMinus(1.0)
    }


    @Test
    fun `it should allow collecting events by type`() = createTestSimulation {
        ClockSync(500.milliseconds)

        val creations = collect<EntityCreatedEvent>()
        val cg = ComponentGenerator(exponential(1), total = 10) { Component() }

        run(10)

        creations.size shouldBe (cg.total + 1) // +1 because of main
    }

    @Suppress("DEPRECATION")
    @Test
    fun `it still support configuring dependencies before creating the simulation`() {
        class Car : Component() {
            override fun process() = sequence {
                hold(10.minutes)
            }
        }

        val env = createSimulation {
            dependency { Car() }

            run(5.minutes)

            get<Car>().isScheduled shouldBe true
        }
    }

    @Test
    fun `it still should fail when last element in createSimulation is a dependency as this will fail internally otherwise`() {
        shouldThrow<IllegalArgumentException> {
            val sim = createSimulation {
                dependency("foo") {  1 }
                dependency("bar") {  2 }
            }

            sim.get<String>("foo")
            sim.run(1.days)
        }
    }

    @Test
    fun `it should fail with exception if simulation is too slow `() {
        createSimulation {
            enableComponentLogger()

            object : TickedComponent() {
                var waitCounter = 1

                override fun repeatedProcess() = sequence {
                    hold(1)
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

            object : TickedComponent() {
                override fun process() =
                    sequence {
                        hold(10)
                    }
            }

            run(until = null as Instant?)
            nowTT shouldBe 10.tt

            cc.size shouldBe 1
        }
    }

    @Test
    fun `it should allow to use custom duration units`() {
        val environment = Environment(tickDurationUnit = DurationUnit.DAYS)

        environment.run(4.days)

        environment.nowTT shouldBe TickTime(4.0)
    }

    @Test
    fun `it should run until or duration until has reached`() {
        createSimulation {
            run(until = env.asSimTime(10))
            nowTT shouldBe 10.tt
        }

        createSimulation {
            run(duration = 5)
            run(duration = 5)
            now shouldBe asSimTime(10)
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
            val atm = dependency { Resource("atm", 1) }

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
        atms.fastMap { it.run(100) }

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


    val testDataDir
        get() = File("src/test/resources/stdout/EnvTests").toPath()

    @Test
    fun `it should log events as json`() {
        captureOutput {
            val er = EmergencyRoom()
            er.trackingPolicyFactory.enableAll()


            er.addEventListener {
//                println(GSON.toJson(it))
                println(it.toJson())
            }

            er.run(1.hours)
        }.stdout shouldBeDiff (testDataDir / "EnvTests_it_should_log_events_as_json.txt").toFile().readText()
    }


    // https://github.com/holgerbrandl/kalasim/issues/49
    @Ignore
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
        data class Tester(val created: Long = System.nanoTime())

        val myModule = module(createdAtStart = true) {
            single(createdAtStart = true) {
                Tester()
            }
        }

        val koinApplication = koinApplication {}
        val koin = koinApplication.koin
        koin.loadModules(modules = listOf(myModule))

        val now = System.nanoTime()

        sleep(10)
        now shouldBeGreaterThan koin.get<Tester>().created
    }

    @Test
    fun `it should create components on simulation start`() {
        val deps = declareDependencies {
            // register components needed for dependency injection
            dependency {
                println("foo")
                ComponentQueue<Customer>("waitingline")
            }
            dependency { CustomerGenerator(get()) }
            dependency { (1..3).map { Clerk() } }
        }

        createSimulation(dependencies = deps) {
            run(50000.0)

            val waitingLine: ComponentQueue<Customer> = get()
            waitingLine.creationTime.epochSeconds shouldBe 0

            println(waitingLine.statistics.toJson())
        }
    }
}