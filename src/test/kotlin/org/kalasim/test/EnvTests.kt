package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import krangl.cumSum
import krangl.mean
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.junit.Test
import org.kalasim.*
import org.kalasim.misc.DependencyContext
import org.kalasim.misc.cartesianProduct
import org.kalasim.misc.fastMap
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
                        while (true) {
                            hold(1)
                            // doe something insanely complex that takes 2seconds
                            Thread.sleep(waitCounter++ * 1000L)
                        }
                    }
            }

            ClockSync(Duration.ofSeconds(1), maxDelay = Duration.ofSeconds(3))

            shouldThrow<ClockOverloadException> {
                run(10)
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

}