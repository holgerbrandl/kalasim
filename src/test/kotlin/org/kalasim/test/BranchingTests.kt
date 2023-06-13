@file:OptIn(AmbiguousDuration::class)

package org.kalasim.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.State
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.tt
import kotlin.time.Duration.Companion.minutes


class Tool : Component() {

    val state = State(false)

    override fun process() = sequence {
        object : Component("branch") {
            override fun process() = sequence {
                shouldThrow<IllegalArgumentException> {
                    doSomething()
                }

//                now shouldBe 50.tt
            }
        }

        // doSomething()
        // ^^ not needed to reproduce the effect

        hold(100.minutes, "busy for another some ticks")

        now shouldBe 100.tt

        println(env.toJson())
    }


    private suspend fun SequenceScope<Component>.doSomething() {
        wait(state, true)
        hold(50, "doing something")
    }
}

//class ProcessScope(val foo: SequenceScope<Component>) {
//    @OptIn(ExperimentalTypeInference::class)
//    public fun <T> sequence(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = foo.seq
//
//}

class BranchingTests {

    @Test
    fun `it should evolve a branch independently from the callee`() = createTestSimulation {
        val tool = Tool()

        run(300)

        tool.isData shouldBe true

        println(now)
    }
}