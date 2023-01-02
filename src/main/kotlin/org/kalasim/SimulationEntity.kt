package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.analysis.snapshot.EntitySnapshot
import org.kalasim.misc.*
import org.koin.core.Koin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.time.Duration


/** Base class of all main simulation entities such as environments, resources, components, states and collections. */
abstract class SimulationEntity(name: String? = null, val simKoin: Koin = DependencyContext.get()) : SimContext,
    WithJson {

    final override val env = getKoin().get<Environment>()

    /** The (possibly auto-generated) name of this simulation entity.*/
    val name = name?.run {
        if(endsWithCDU()) defaultName(env.nameCache) else this
    } ?: javaClass.defaultName(env.nameCache)

    /** return true if name ends with dot, dash or underscore. Used for auto-index pattern detection.*/
    private fun String.endsWithCDU(): Boolean = endsWith('.') || endsWith('-') || endsWith('_')

    /** The time when the component was instantiated. */
    val creationTime = env.now

    open val snapshot: EntitySnapshot = object : EntitySnapshot {
        override fun toJson() = json {
            "name" to name
            "time" to now
        }
    }

    override fun toJson() = snapshot.toJson()

    /** Print info about this resource */
    internal fun printInfo() = println(snapshot.toJson().toIndentString())

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


    override var tickTransform: TickTransform
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
     * @param event A structured event log record. Users may want to sub-class `Event` to provide their
     *              own structured log record formats.
     */
    // note do not use internally (because not managed by tracking-configuration)
    fun log(event: Event) {
        env.publishEvent(event)
    }


    internal fun log(enabled: Boolean, builder: () -> Event) {
        if(enabled) {
            log(builder())
        }
    }
}


//
// Auto-Naming
//

// To be removed in v0.9 (unclear intent & no call-sites)
//internal fun Any.nameOrDefault(name: String?, nameCache: MutableMap<String, Int>) =
//    name ?: this.javaClass.defaultName(nameCache)

internal fun Class<*>.defaultName(nameCache: MutableMap<String, Int>) =
    simpleName.ifEmpty { "Component" } + "." + getComponentCounter(simpleName, nameCache)

private fun String.defaultName(nameCache: MutableMap<String, Int>) =
    this + getComponentCounter(this.removeRange(length-1 until length), nameCache)

private fun getComponentCounter(className: String, nameCache: MutableMap<String, Int>) =
    nameCache.merge(className, 1, Int::plus)

