package org.kalasim.examples

import org.kalasim.*
import org.koin.core.component.get


val ttf = uniform(19, 20) // time to failure distribution
val ttr = uniform(3, 6)  //  time to repair distribution


class Part(val machine: Machine, partNo: Int) :
    Component(
        name = machine.name.replace("Machine", "part") + ".${partNo + 1}"
    ) {

    override fun process() = sequence {

        while (true) {
            hold(ttf())
            machine.interrupt()
            hold(ttr())
            machine.resume()
        }
    }
}


class Machine : Component() {

    init {
        repeat(3) { Part(this, it) }
    }

    override fun process() = sequence {
        while (true) {
            val r = get<Resource>()
            request(r)
            hold(5)
            release(r)
        }
    }
}

fun main() {
    declareDependencies {
        add { Resource() }
    }.createSimulation(true) {
        repeat(2) { Machine() }

        run(400)
    }
}