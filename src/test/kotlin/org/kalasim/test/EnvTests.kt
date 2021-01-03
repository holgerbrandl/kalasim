package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.junit.Test
import org.kalasim.*
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.dsl.koinApplication

class EnvTests {

    @Test
    fun `it should support more than one env`() {
        GlobalContext.stop()

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
            GlobalContext.get()
        }
    }

    @Test
    fun `it should run be possible to run an old`() {

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
        env1.addTraceListener { println(it) }
        shouldThrow<IllegalStateException> {
            env1.run(10)
        }

        println(env1._koin)
        println(env2._koin)
    }
}