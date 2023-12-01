package org.kalasim.examples.shipyard

import io.github.oshai.kotlinlogging.KotlinLogging
import org.kalasim.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

class Part(
    val partId: String,
    val makeTime: Duration,
    /** Standard deviation of the makespan. */
    val makeTimeSD: Duration,
    vararg val components: Part,
    val finalProduct: Boolean = false
) {

    fun computeMinimalMakespan(): Duration {
        return makeTime - (makeTimeSD ) + (components.maxOfOrNull { it.computeMinimalMakespan() } ?: Duration.ZERO)
    }
}

// todo get rid of time attribute
class PartCompleted(time: SimTime, val part: Part) : Event(time)

class PartAssembly(val part: Part) : Component() {
    val completed = State(false)

    val orgTime = normal(10, 3).minutes

    override fun process() = sequence {
        hold(orgTime())

        // branch part production
        val partAssemblies = part.components.map { PartAssembly(it) }

        // 1) join parts manually
//        val assemblyStates = partAssemblies.map { it.componentState() }
//        wait(*assemblyStates.map { it turns ComponentState.PASSIVE }.toTypedArray())
//            wait(*assemblyStates.map {  it turns true }.toTypedArray())

        // 2) make parts with utility method to join sub-processes
        join(partAssemblies)

        // 3) use dedicated state variable in each process
//        wait(*partAssemblies.map {  it.completed turns true }.toTypedArray())


        // assembly them
        // use exponential here? rexp(10, rate=1/10)
        val makeTimeDist = uniform(part.makeTime- part.makeTimeSD, part.makeTime+ part.makeTimeSD)
        val duration = makeTimeDist()

        println(duration)
        hold(duration)

//        logger.info { "completed assembly of part ${this}" }

        completed.value = true

        // indicate completion
        log(PartCompleted(now, part))
    }
}


class Shipyard : Environment(tickDurationUnit = DurationUnit.DAYS) {

    val logger = KotlinLogging.logger {}

    // utility to model rectified normal distribution with a fifth of the mean as sd
//    fun norm5(mean: Duration)= normal(mean, mean/5, true)

    fun configureOrders(bom: List<Part> = exampleBOM(), iat: DurationDistribution = normal(24.hours, 2.hours, true)) {
        ComponentGenerator(iat) {
            bom.random()
        }.addConsumer {
            PartAssembly(it).componentState().onChange {
                println("state changed ${it.value}")
            }
        }
    }

    fun exampleBOM(): List<Part> {

        val hullFront = Part("hull-front", 2.days, 1.days)
        val hullBack = Part("hull-back", 3.days, 1.days)

        val rumpf = Part("rumpf", 1.days,  0.5.days, hullFront, hullBack)
        val deck = Part("deck", 2.days, 0.2.days)
        val bridge = Part("bridge", 3.days, 1.days)
        val kitchen = Part("kitchen", 3.days, 1.days)
        val lackieren = Part("lackieren", 3.days, 1.days, rumpf, deck)


        // subcomponents
        val products = listOf(
            Part("ship1", 5.days, 1.days,  lackieren, deck, bridge, finalProduct = true),
            Part("ship2", 3.days, 0.5.days,  lackieren, bridge, finalProduct = true),
            Part("ship3", 3.days, 0.5.days,  bridge, deck, kitchen, finalProduct = true)
        )

        return products
    }

}


fun main() {
    Shipyard().apply {
        configureOrders()

        enableComponentLogger()

        run(3.weeks)
    }
}