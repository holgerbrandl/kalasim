@file:Suppress("EXPERIMENTAL_OVERRIDE")

package org.kalasim

import org.kalasim.misc.Jsonable
import org.kalasim.misc.printThis
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext


@Suppress("EXPERIMENTAL_API_USAGE")
abstract class SimulationEntity(name: String?, val simKoin : Koin = GlobalContext.get())
    :    KoinComponent
{
//    val env by lazy { getKoin().get<Environment>() }
    val env = getKoin().get<Environment>()

//    var name: String
//        private set

    val name = nameOrDefault(name, env.nameCache)

    val creationTime = env.now

    var monitor = true;

//    init {
//        this.name = nameOrDefault(name)
//    }

    //    abstract fun getSnapshot(): Snapshot
    protected abstract val info: Jsonable

    /** Print info about this resource */
    fun printInfo() = info.printThis()

    override fun toString(): String = "${javaClass.simpleName}($name)"


    //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3
    final override fun getKoin(): Koin = simKoin


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


private fun getComponentCounter(className: String, nameCache: MutableMap<String, Int>) =
    nameCache.merge(className, 1, Int::plus)

internal fun Any.nameOrDefault(name: String?, nameCache: MutableMap<String, Int>) =
    name ?: this.javaClass.defaultName(nameCache)

internal fun Class<Any>.defaultName(nameCache: MutableMap<String, Int>) =
    simpleName + "." + getComponentCounter(simpleName, nameCache)
