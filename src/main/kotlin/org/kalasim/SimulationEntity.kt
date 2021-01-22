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

    /** The (possibly auto-generated) name of this simulation entity.*/
    val name = name ?: javaClass.defaultName(env.nameCache)

    /** The time when the component was instantiated. */
    val creationTime = env.now

    /** Indicates if log events from this entity should be tracked. */
    var monitor = true;

    protected abstract val info: Jsonable

    /** Print info about this resource */
    fun printInfo() = info.printThis()

    override fun toString(): String = "${javaClass.simpleName}($name)"


    //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3
    final override fun getKoin(): Koin = simKoin


    /**
     * Records a state-change event.
     *
     * @param action Describing the nature if the event
     */
    fun log(action: String) = env.apply { log(now, curComponent, this@SimulationEntity, action) }


    /**
     * Records a state-change event.
     *
     * @param source Modification causing simulation entity
     * @param action Describing the nature if the event
     * @param details More details characterizing the state change
     */
    fun <T : SimulationEntity> log(source: T, action: String?, details: String? = null) =
        env.apply { log(now, curComponent, source, action, details) }


    /**
     * Records a state-change event.
     *
     * @param time The current simulation time
     * @param curComponent  Modification consuming component
     * @param source Modification causing simulation entity
     * @param action Describing the nature if the event
     * @param details More details characterizing the state change
     */
    fun log(
        time: Double,
        curComponent: Component?,
        source: SimulationEntity?,
        action: String? = null,
        details: String? = null
    ) {
        if(monitor) log(DefaultEvent(time, curComponent, source, action, details))
    }


    /**
     * Records a state-change event.
     *
     * @param event A structure event log record. Users may want to sub-class `Event` to provide their
     *              own structured log record formats.
     */
    fun log(event: Event) {
        if(monitor) env.publishEvent(event)
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
