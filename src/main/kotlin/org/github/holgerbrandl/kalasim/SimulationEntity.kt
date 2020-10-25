package org.github.holgerbrandl.kalasim

import org.koin.core.KoinComponent

abstract class SimulationEntity(name: String?) : KoinComponent {
    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    init {
        this.name = nameOrDefault(name)
        env.printTrace("create ${this.name}")
    }
}


//
// Auto-Naming
//

private val componentCounters = mapOf<String, Int>().toMutableMap()

private fun getComponentCounter(className: String) = componentCounters.merge(className, 1, Int::plus)

internal fun Any.nameOrDefault(name: String?) =
    name ?: this.javaClass.defaultName()

internal fun Class<Any>.defaultName() = javaClass.simpleName + "." + getComponentCounter(javaClass.simpleName)
