package org.kalasim

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.analysis.snapshot.ComponentGeneratorSnapshot
import org.kalasim.misc.DependencyContext
import org.kalasim.misc.NumericDuration
import org.koin.core.Koin

/**
 * A component generator can be used to generate components.
 *
 * See https://www.salabim.org/manual/ComponentGenerator.html
 *
 * There are two ways of generating components:
 *  * according to a given inter arrival time (iat) value or distribution
 *  * random spread over a given time interval
 *
 *  @param iat Inter arrival time arrival time distribution between history/generations.
 *  @param startAt time where the generator starts time. If omitted, `now` is used.
 *  @param forceStart If `false` (default), the first component will be generated at `time = startAt + iat()`. If `true`, the first component will be generated at `startAt`.
 *  @param until time up to which components should be generated. If omitted, no end.
 *  @param total (maximum) number of components to be generated.
 *  @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)
 * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
 *  @param keepHistory If `true`, i will store a reference of all generated components which can be queried with `history`.
 * @param koin The dependency resolution context to be used to resolve the `org.kalasim.Environment`
 */
class ComponentGenerator<T>(
    val iat: RealDistribution,
    val startAt: TickTime? = null,
    val forceStart: Boolean = false,
    var until: TickTime = TickTime(Double.MAX_VALUE),
    val total: Int = Int.MAX_VALUE,
    name: String? = null,
    priority: Priority = Priority.NORMAL,
    val keepHistory: Boolean = false,
    koin: Koin = DependencyContext.get(),
    val builder: Environment.(counter: Int) -> T
) : Component(name, priority = priority, at = startAt, process = ComponentGenerator<T>::doIat, koin = koin) {

    val history: List<T> = mutableListOf<T>()

    fun interface CompGenObserver<K> {
        fun consume(generated: K)
    }

    private val consumers = mutableListOf<CompGenObserver<T>>()
    fun addConsumer(consumer: CompGenObserver<T>) = consumers.add(consumer)
    @Suppress("unused")
    fun removeConsumer(consumer: CompGenObserver<T>) = consumers.remove(consumer)

    var numGenerated = 0
        private set

    @OptIn(NumericDuration::class)
    fun doIat(): Sequence<Component> = sequence {

        val iatSeq = sequence { if (forceStart) yield(0.0); while (true) yield(iat()) }.iterator()

        while (true) {
            val interArrivalTime = iatSeq.next()

            if ((env.now + interArrivalTime) > until || isData) {
//                yield(activate(at = until, process = ComponentGenerator<T>::doFinalize))
                break
            }

            hold(interArrivalTime)

            val created = builder(env, numGenerated)
            numGenerated++

            consumers.forEach { it.consume(created) }

            if (keepHistory) (history as MutableList<T>).add(created)

            if (numGenerated >= total) break
        }
    }

//    private fun doFinalize(): Sequence<Component> = sequence {
//        log(env.now, env.curComponent, this@ComponentGenerator, "till reached")
//    }

     override val snapshot: ComponentGeneratorSnapshot<T>
        get() = ComponentGeneratorSnapshot(this)
}
