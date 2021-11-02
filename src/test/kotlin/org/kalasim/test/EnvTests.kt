package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.junit.Test
import org.kalasim.*
import org.kalasim.misc.DependencyContext
import org.koin.core.Koin
import org.koin.core.component.get
import org.koin.core.error.NoBeanDefFoundException
import org.koin.dsl.koinApplication
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
        shouldThrow<IllegalStateException> {
            DependencyContext.get()
        }
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
    fun `it should allow to synchronize clock time`() {
        val timeBefore = System.currentTimeMillis()

        createSimulation(true) {
            ClockSync(Duration.ofMillis(500))

            run(10)
        }

        (System.currentTimeMillis() - timeBefore) / 1000.0 shouldBe 5.0.plusOrMinus(1.0)
    }

    @Test
    fun `it should fail with exception if simulation is too slow `() {
        createSimulation(true) {
            object : Component() {
                var waitCounter = 1
                override fun process() = sequence {
                    while(true) {
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
            object : Component() {
                override fun process() = sequence<Component> {
                    hold(10)
                }
            }

            run(until = null)
            now shouldBe 10.tt
        }
    }

    @Test
    fun `it should run until or duration until has reached`() {
        createSimulation {
            run(until = TickTime(10))
            now shouldBe 10.tt
        }

        createSimulation {
            run(ticks = 5)
            run(ticks = 5)
            now shouldBe 10.tt
        }
    }
}