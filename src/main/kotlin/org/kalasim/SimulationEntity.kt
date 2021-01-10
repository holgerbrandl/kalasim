@file:Suppress("EXPERIMENTAL_OVERRIDE")

package org.kalasim

import org.kalasim.misc.Jsonable
import org.kalasim.misc.printThis
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext


@Suppress("EXPERIMENTAL_API_USAGE")
abstract class SimulationEntity(name: String?, val simKoin: Koin = GlobalContext.get()) : KoinComponent {
    val env = getKoin().get<Environment>()

    val name = name ?: javaClass.defaultName(env.nameCache)

    val creationTime = env.now

    var monitor = true;

    protected abstract val info: Jsonable

    /** Print info about this resource */
    fun printInfo() = info.printThis()

    override fun toString(): String = "${javaClass.simpleName}($name)"


    //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3
    final override fun getKoin(): Koin = simKoin


    fun printTrace(info: String) = env.apply { printTrace(now, curComponent, this@SimulationEntity, info) }

    fun <T : Component> printTrace(element: T, info: String?, details: TraceDetails?=null) =
        env.apply { printTrace(now, curComponent, element, info, null) }

    /**
     * Prints a trace line
     *
     *  @param curComponent  Modification consuming component
     *  @param source Modification causing simulation entity
     *  @param action Detailing out the nature of the modification
     */
    fun printTrace(
        time: Double,
        curComponent: Component?,
        source: SimulationEntity?,
        action: String? = null,
        actionDetails: String? = null
        
    ) {
        if (!monitor) return

        val tr = TraceElement(time, curComponent, source, action, actionDetails)

        publishTraceRecord(tr)
    }

    protected fun publishTraceRecord(tr: TraceElement) {
        if (!monitor) return

        env.publishTraceRecord(tr)
    }
}


//
// Auto-Naming
//

internal fun Any.nameOrDefault(name: String?, nameCache: MutableMap<String, Int>) =
    name ?: this.javaClass.defaultName(nameCache)

internal fun Class<*>.defaultName(nameCache: MutableMap<String, Int>) =
    simpleName + "." + getComponentCounter(simpleName, nameCache)

private fun getComponentCounter(className: String, nameCache: MutableMap<String, Int>) =
    nameCache.merge(className, 1, Int::plus)
