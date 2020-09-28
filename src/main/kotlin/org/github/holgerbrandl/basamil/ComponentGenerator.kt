package org.github.holgerbrandl.basamil

import org.apache.commons.math3.distribution.RealDistribution

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
    val from: Double? = 0.0,
    var till: Double = Double.MAX_VALUE,
    val iat: RealDistribution,
    val total: Int = Int.MAX_VALUE,
    env: Environment,
    name: String? = null,
    priority: Int = 0,
    urgent: Boolean = false,
    val builder: () -> T
) : Component(env, name, priority = priority, process = ComponentGenerator<T>::doIat) {


    init {
        // TODO build intervals

    }

    fun doIat(): Sequence<Component> = sequence {
        var numGenerated = 0

        while (true) {
            val c = builder()
            numGenerated++

            if (numGenerated >= total) break

            val t = env.now() + iat.sample()

            if (t > till) {
                yield((this@ComponentGenerator).activate(process = ComponentGenerator<T>::doFinalize, at = till))
            }

            yield(hold(till=t))
        }
    }

    fun doFinalize(): Sequence<Component> = sequence {
        env.printTrace(now(), this@ComponentGenerator, "till reached")
    }

    fun printInfo(): Nothing = TODO("Implement me")
}