package org.kalasim.test

import org.junit.Test
import org.kalasim.*
import kotlin.time.Duration.Companion.minutes

class IssueWithReflection {

    @Test
    fun `it should test something`() = createTestSimulation {

        data class Recipe(val name: String)

        class Kitchen : Component() {

            fun lunchTime(): Sequence<Component> = sequence {
                hold(15.minute)

                activate(::cookSomethingSpecial, Recipe("cake"), true, delay = 5.minutes)
            }


            fun cookSomethingSpecial(recipe: Recipe, spicy: Boolean) = sequence {
                hold(15.minutes, "cooking...")

                log("dinner's ready! I am serving ${if(spicy) "spicy" else ""} $recipe today")
            }
        }

        val kitchen = Kitchen()


        // test that we can activate a process with args from within process
        kitchen.activate(process = Kitchen::lunchTime)
        run()
    }
}
