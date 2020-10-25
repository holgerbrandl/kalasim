package org.github.holgerbrandl.kalasim.scratch

import org.koin.core.context.startKoin
import org.koin.dsl.module

class Core {
    init{ println("Init Core")}
}

class ZZZ {
    init{ println("Init ZZZ")}
}

fun main() {
    val coreModule = module { single(createdAtStart = true) { Core() } }


    val ka = startKoin { modules(coreModule) }

    val userModule = module { single(createdAtStart = true) { ZZZ() } }
    ka.koin.loadModules(listOf(userModule))

    println("finished setup")

    val foo = ka.koin.get<ZZZ>()
}