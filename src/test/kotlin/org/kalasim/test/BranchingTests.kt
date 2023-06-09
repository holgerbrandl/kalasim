@file:OptIn(AmbiguousDuration::class)

package org.kalasim.test

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.kalasim.Component
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.printThis
import org.kalasim.tt


class Tool : Component(){

    override fun process() = sequence {
        object : Component("branch"){
            override fun process()  = sequence {
                doSomething()
                now shouldBe 50.tt
            }
        }

        // doSomething()
        // ^^ not needed to reproduce the effect

        hold(100, "busy for another some ticks")

        now shouldBe 100.tt

        println(env.toJson())
    }

     suspend fun SequenceScope<Component>.doSomething(){
        hold(50, "doing something")
    }
}

class BranchingTests {

    @Test
    fun `it should evolve a branch independently from the callee`() = createTestSimulation {
        val tool = Tool()

        run(300)

        tool.isData shouldBe true

        println(now)
    }
}