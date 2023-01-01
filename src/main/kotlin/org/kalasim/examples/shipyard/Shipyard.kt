package org.kalasim.examples.shipyard

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit


class Shipyard : Environment(DurationUnit.DAYS) {

    class Part(val partId: String,  val duration: RealDistribution, vararg val components: Part) : SimulationEntity()



    val rumpf = Part("rumpf", duration = normal(1))
    val deck = Part("deck", duration = normal(2))
    val segment = Part("segment", duration = normal(5))
    val bridge = Part("bridge", duration = normal(3))
    val kitchen = Part("kitchen", duration = normal(3))
    val lackieren = Part("lackieren", duration = normal(3), segment, deck)


    // subcomponents
    val products = listOf(
        Part("ship1", normal(5, 2), rumpf, deck, bridge),
        Part("ship2", normal(2, 3), rumpf, rumpf, rumpf),
        Part("ship3", normal(2, 3), rumpf, bridge, deck)
    )

    // todo get rid of time attribute
    class PartCompleted(time: TickTime, val part: Part) : Event(time)


    class PartAssembly(val part: Part) : Component() {
//        val completed =  State(false)

        val orgTime = normal(10, 3).minutes

        override fun process() = sequence {
            hold(orgTime())

            // branch part production
            val assemblyStates = part.components.map { component ->
                PartAssembly(component).componentState()
//                PartAssembly(component).completed
            }

            // join parts
            wait(*assemblyStates.map { it turns ComponentState.PASSIVE }.toTypedArray())
//            wait(*assemblyStates.map {  it turns true }.toTypedArray())

            // assembly them
            hold(part.duration().days)


//            completed.value = true

            // indicate completion
            log(PartCompleted(now, part))
        }
    }

    init {
        ComponentGenerator(normal(23, 2)) {
            products.random()
        }.addConsumer {
            PartAssembly(it).componentState().onChange{
                println("state changed ${it.value}")
            }
        }
    }
}

class Car : Component() {
    override fun repeatedProcess() = sequence<Component> {
        listOf(1, 2, 3).random()
    }
}

fun main() {
    Shipyard().apply {
        enableConsoleLogger()

        run(3.minutes)
    }
}