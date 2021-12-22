@file:Suppress("EXPERIMENTAL_OVERRIDE")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.json.JSONObject
import org.kalasim.analysis.InteractionEvent
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.component.*
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier


@Suppress("EXPERIMENTAL_API_USAGE")
abstract class SimulationEntity(name: String? = null, val simKoin: Koin = DependencyContext.get()) : SimContext {
    val env = getKoin().get<Environment>()

    /** The (possibly auto-generated) name of this simulation entity.*/
    val name = name ?: javaClass.defaultName(env.nameCache)

    /** The time when the component was instantiated. */
    val creationTime = env.now


    open val info: Jsonable = object : Jsonable() {
        override fun toJson(): JSONObject {
            return json { "name" to name }
        }
    }


    /** Print info about this resource */
    fun printInfo() = info.printThis()

    //    override fun toString(): String = "${javaClass.simpleName}($name)"
    override fun toString(): String = name


    //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3
    final override fun getKoin(): Koin = simKoin


    //redeclare to simplify imports
    /** Resolves a dependency in the simulation. Dependencies can be disambiguated by using a qualifier.*/
    inline fun <reified T : Any> get(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ): T =
        getKoin().get(qualifier, parameters)


    internal fun logInternal(enabled: Boolean, action: String) = log(enabled) {
        with(env) { InteractionEvent(now, curComponent, this@SimulationEntity, action) }
    }

    override var tickTransform: TickTransform?
        get() = env.tickTransform
        set(_) {
            throw RuntimeException("Tick transformation must be set via the environment")
        }

    /** The current simulation time*/
    val now
        get() = env.now
//        private set


    val random
        get() = env.random

    /**
     * Records a state-change event.
     *
     * @param source Modification causing simulation entity
     * @param action Describing the nature if the event
     * @param details More details characterizing the state change
     */
//    fun <T : SimulationEntity> log(source: T, action: String?, details: String? = null) =
//        env.apply { log(now, curComponent, source, action, details) }


    protected fun logCreation(created: SimulationEntity?, details: String? = null) {
        log(InteractionEvent(now, env.curComponent, created, details))
    }


    /**
     * Records a state-change event.
     *
     * @param action Describing the nature if the event
     */
    fun log(action: String) = env.apply { log(InteractionEvent(now, curComponent, this@SimulationEntity, action)) }


    /**
     * Records a state-change event.
     *
     * @param event A structured event log record. Users may want to sub-class `Event` to provide their
     *              own structured log record formats.
     */
    // note do not use internally (because not managed by tracking-configuration)
    fun log(event: Event) {
        env.publishEvent(event)
    }


    internal fun log(enabled: Boolean, builder: () -> Event) {
        if (enabled) {
            log(builder())
        }
    }
}


//
// Auto-Naming
//

internal fun Any.nameOrDefault(name: String?, nameCache: MutableMap<String, Int>) =
    name ?: this.javaClass.defaultName(nameCache)

internal fun Class<*>.defaultName(nameCache: MutableMap<String, Int>) =
    simpleName.ifEmpty { "Component" } + "." + getComponentCounter(simpleName, nameCache)

private fun getComponentCounter(className: String, nameCache: MutableMap<String, Int>) =
    nameCache.merge(className, 1, Int::plus)
