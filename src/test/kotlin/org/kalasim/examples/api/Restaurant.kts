//Restaurant.kts
import org.kalasim.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class Recipe(val name: String) {
    override fun toString() = name
}

class Customer : Component() {
    val restaurant  = get<Restaurant>()

    override fun process() = sequence<Component> {
        hold(3.hours + 15.minutes) // arrive somewhen

        restaurant.activate(process=Restaurant::cookSomething, Recipe("pasta"))

        hold(30.minutes,"drink something while waiting for food")

        // wait for the food preparation to complete
        join(restaurant)

        //order something else
        restaurant.activate(process=Restaurant::specialOffer)
    }
}

class Restaurant : Component() {

    override fun process(): Sequence<Component> = sequence {
        hold(2.hours, "opening restaurant")
    }

    fun cookSomething(recipe: Recipe) = sequence {
        hold(10.minutes, "preparing $recipe")

        log("dinner's ready! I am serving $recipe today")
    }

    fun specialOffer(): Sequence<Component> = sequence {
        hold(5.minutes, "selecting dish of the day")

        // We can activate another process from within a process definition
        // Provide a recipe, set the `spicy` flag and delay activation by 5 minutes
        activate(::cookSomethingSpecial, Recipe("cake"), true, delay = 5.minutes)
    }

    fun cookSomethingSpecial(recipe: Recipe, spicy: Boolean) = sequence {
        hold(20.minutes, "preparing $recipe ...")

        yieldAll(prepareDesert()) // inline sub-process

        log("special dinner's ready!")
        log("serving ${if(spicy) "spicy" else ""} $recipe and some desert")
    }

    // another small process without any arguments
    fun prepareDesert() = sequence {
        hold(15.minutes, "making a pie")
    }
}

createSimulation {
    enableComponentLogger()

    // instantiate the simulation components
    val restaurant = dependency { Restaurant() }

    // create customer
    Customer()

    // run the model
    run()

    // activate process with arguments from outside a process definition
    restaurant.activate(
        process = Restaurant::cookSomething,
        processArgument = Recipe("lasagne")
    )

    // and run again
    run()
}

