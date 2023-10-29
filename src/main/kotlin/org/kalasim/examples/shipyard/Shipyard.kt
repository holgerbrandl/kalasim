package org.kalasim.examples.shipyard

import io.github.oshai.kotlinlogging.KotlinLogging
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

class Part(val partId: String, val makeTime: DurationDistribution, vararg val components: Part) :
    SimulationEntity(partId)

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
        hold(part.makeTime())

        logger.info { "completed assembly of part ${this}" }

        completed.value = true

        // indicate completion
        log(PartCompleted(now, part))
    }
}


class Shipyard() : Environment(tickDurationUnit = DurationUnit.DAYS) {

    val logger = KotlinLogging.logger {}

    // utility to model rectified normal distribution with a fifth of the mean as sd
//    fun norm5(mean: Duration)= normal(mean, mean/5, true)

    fun configureOrders(bom: List<Part> = exampleBOM(), iat: DurationDistribution = normal(24, 2).hours) {
        ComponentGenerator(iat) {
            bom.random()
        }.addConsumer {
            PartAssembly(it).componentState().onChange {
                println("state changed ${it.value}")
            }
        }

    }

    fun exampleBOM(): List<Part> {

        val rumpf = Part("rumpf", normal(1.days, 0.2.days, true))
        val deck = Part("deck", normal(2.days, 0.2.days, true))
        val segment = Part("segment", normal(5.days, 2.days, true))
        val bridge = Part("bridge", normal(3.days, 1.days, true))
        val kitchen = Part("kitchen", normal(3.days, 1.days, true))
        val lackieren = Part("lackieren", normal(3.days, 1.days, true), segment, deck)


        // subcomponents
        val products = listOf(
            Part("ship1", normal(5.days, 2.days, rectify = true), rumpf, deck, bridge),
            Part("ship2", normal(3.days, 1.days, rectify = true), rumpf, rumpf, rumpf),
            Part("ship3", normal(3.days, 1.days, rectify = true), rumpf, bridge, deck)
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