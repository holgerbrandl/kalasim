package org.kalasim

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.misc.Jsonable
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

/**
 * A component generator can be used to genetate components
 *
 * See https://www.salabim.org/manual/ComponentGenerator.html
 *
 * There are two ways of generating components:
 *  * according to a given inter arrival time (iat) value or distribution
 *  * random spread over a given time interval
 *
 *  @param from time where the generator starts time
 *  @param till time up to which components should be generated. If omitted, no end
 */
class ComponentGenerator<T : Component>(
    val iat: RealDistribution,
    val from: Double? = 0.0,
    var till: Double = Double.MAX_VALUE,
    val total: Int = Int.MAX_VALUE,
    name: String? = null,
    priority: Int = 0,
    @Suppress("UNUSED_PARAMETER") urgent: Boolean = false,
    koin: Koin = GlobalContext.get(),
    val builder: Environment.(counter: Int) -> T
) : Component(name, priority = priority, process = ComponentGenerator<T>::doIat, koin = koin) {


    init {
        // TODO build intervals

    }

    fun doIat(): Sequence<Component> = sequence {
        var numGenerated = 0

        while (true) {
            builder(env, numGenerated)
            numGenerated++

            if (numGenerated >= total) break

            val t = env.now + iat.sample()

            if (t > till) {
                yield((this@ComponentGenerator).activate(at = till, process = ComponentGenerator<T>::doFinalize))
            }

            hold(till = t)
        }
    }

    fun doFinalize(): Sequence<Component> = sequence {
        printTrace(env.now, env.curComponent, this@ComponentGenerator, "till reached")
    }

    override val info: Jsonable
        get() = ComponentGeneratorInfo(this)
}


class ComponentGeneratorInfo<T : Component>(cg: ComponentGenerator<T>) : ComponentInfo(cg)