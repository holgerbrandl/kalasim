package org.kalasim.examples.shipyard

import org.kalasim.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit


class Shipyard : Environment(DurationUnit.DAYS) {

    class Part(val partId: String, val makeTime: DurationDistribution, vararg val components: Part) : SimulationEntity()

    // utilty to model rectified normal distribution wiht a fifth of the mean as sd
//    fun norm5(mean: Duration)= normal(mean, mean/5, true)

    val rumpf = Part("rumpf",  normal(1.days, 0.2.days, true))
    val deck = Part("deck", normal(2.days, 0.2.days, true))
    val segment = Part("segment", normal(5.days, 2.days, true))
    val bridge = Part("bridge", normal(3.days, 1.days, true))
    val kitchen = Part("kitchen", normal(3.days, 1.days, true))
    val lackieren = Part("lackieren", normal(3.days, 1.days, true), segment, deck)


    // subcomponents
    val products = listOf(
        Part("ship1", normal(5.days, 2.days), rumpf, deck, bridge),
        Part("ship2", normal(2.days, 3.days), rumpf, rumpf, rumpf),
        Part("ship3", normal(2.days, 3.days), rumpf, bridge, deck)
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
            hold(part.makeTime())

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
        enableComponentLogger()

        run(3.minutes)
    }
}