package org.github.holgerbrandl.kalasim

import org.koin.core.KoinComponent

abstract class SimulationEntity(name: String?) : KoinComponent {
    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    val creationTime  = env.now

    init {
        this.name = nameOrDefault(name)
    }

//    abstract fun getSnapshot(): Snapshot
    protected abstract val info: Snapshot

    fun printInfo() = info.println()
    override fun toString(): String = "${javaClass.simpleName}($name)"
}


//
// Auto-Naming
//

private val componentCounters = mapOf<String, Int>().toMutableMap()

private fun getComponentCounter(className: String) = componentCounters.merge(className, 1, Int::plus)

internal fun Any.nameOrDefault(name: String?) =
    name ?: this.javaClass.defaultName()

internal fun Class<Any>.defaultName() = simpleName + "." + getComponentCounter(simpleName)
