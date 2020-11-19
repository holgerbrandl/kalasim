package org.github.holgerbrandl.kalasim

import org.github.holgerbrandl.kalasim.misc.println
import org.koin.core.KoinComponent

abstract class SimulationEntity(name: String?) : KoinComponent {
    val env by lazy { getKoin().get<Environment>() }

    var name: String
        private set

    val creationTime = env.now

    var monitor = true;

    init {
        this.name = nameOrDefault(name)
    }

    //    abstract fun getSnapshot(): Snapshot
    protected abstract val info: Snapshot

    fun printInfo() = info.println()

    override fun toString(): String = "${javaClass.simpleName}($name)"


    fun printTrace(info: String) = env.apply { printTrace(now, curComponent, null, null, info) }

    fun <T : Component> printTrace(element: T, info: String?) =
        env.apply { printTrace(now, curComponent, element, null, info) }

    /**
     * Prints a trace line
     *
     *  @param curComponent  Modification consuming component
     *  @param source Modification causing simulation entity
     *  @param info Detailing out the nature of the modification
     */
    fun printTrace(
        time: Double,
        curComponent: Component?,
        source: SimulationEntity?,
        actionDetails: String?,
        info: String? = null
    ) {
        if (!monitor) return

        val tr = TraceElement(time, curComponent, source, actionDetails, info)

        env.publishTraceRecord(tr)
    }
}


//
// Auto-Naming
//

private val componentCounters = mapOf<String, Int>().toMutableMap()

private fun getComponentCounter(className: String) = componentCounters.merge(className, 1, Int::plus)

internal fun Any.nameOrDefault(name: String?) =
    name ?: this.javaClass.defaultName()

internal fun Class<Any>.defaultName() = simpleName + "." + getComponentCounter(simpleName)
