package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
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
            override suspend fun ProcContext.process() {
                yield(hold(2))
                println("my env is ${env.getKoin()}")
            }
        }

        val env1 = Environment(koin = koinApplication { }.koin)

        env1.apply {

            val component = TestComponent(koin = getKoin())
            Resource(koin = env1.getKoin())
            State<Boolean>(false, koin = getKoin())
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
}