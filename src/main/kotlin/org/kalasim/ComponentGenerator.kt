package org.kalasim

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.analysis.snapshot.ComponentGeneratorSnapshot
import org.kalasim.misc.DependencyContext
import org.kalasim.misc.AmbiguousDuration
import org.koin.core.Koin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A component generator can be used to generate components.
 *
 * For supported arguments see https://www.kalasim.org/component/#component-generator
 */
class ComponentGenerator<T>(
    val iat: DurationDistribution,
    startAt: TickTime? = null,
    val forceStart: Boolean = false,
    var until: TickTime = TickTime(Double.MAX_VALUE),
    val total: Int = Int.MAX_VALUE,
    name: String? = null,
    priority: Priority = Priority.NORMAL,
    val keepHistory: Boolean = false,
    koin: Koin = DependencyContext.get(),
    val builder: Environment.(counter: Int) -> T
) : Component(name, priority = priority, at = startAt, process = ComponentGenerator<T>::doIat, koin = koin) {


    /**
     * A component generator can be used to generate components.
     *
     * For supported arguments see https://www.kalasim.org/component/#component-generator
     */
    @AmbiguousDuration
    constructor(
        iat: RealDistribution,
        startAt: TickTime? = null,
        forceStart: Boolean = false,
        until: TickTime = TickTime(Double.MAX_VALUE),
        total: Int = Int.MAX_VALUE,
        name: String? = null,
        priority: Priority = Priority.NORMAL,
        keepHistory: Boolean = false,
        koin: Koin = DependencyContext.get(),
        builder: Environment.(counter: Int) -> T
    ) : this(
        DurationDistribution(koin.get<Environment>().durationUnit, iat),
        startAt,
        forceStart,
        until,
        total,
        name,
        priority,
        keepHistory,
        koin,
        builder
    )

    /**
     * A component generator can be used to generate components.
     *
     * For supported arguments see https://www.kalasim.org/component/#component-generator
     */
    constructor(
        iat: Duration,
        startAt: TickTime? = null,
        forceStart: Boolean = false,
        until: TickTime = TickTime(Double.MAX_VALUE),
        total: Int = Int.MAX_VALUE,
        name: String? = null,
        priority: Priority = Priority.NORMAL,
        keepHistory: Boolean = false,
        koin: Koin = DependencyContext.get(),
        builder: Environment.(counter: Int) -> T
    ) : this(
        constant(iat),
        startAt,
        forceStart,
        until,
        total,
        name,
        priority,
        keepHistory,
        koin,
        builder
    )

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

    fun doIat(): Sequence<Component> = sequence {

        val iatSeq = sequence { if(forceStart) yield(0.seconds); while(true) yield(iat()) }.iterator()

        while(true) {
            val interArrivalTime = iatSeq.next()

            if((env.now + interArrivalTime) > until || isData) {
//                yield(activate(at = until, process = ComponentGenerator<T>::doFinalize))
                break
            }

            hold(interArrivalTime)

            val created = builder(env, numGenerated)
            numGenerated++

            consumers.forEach { it.consume(created) }

            if(keepHistory) (history as MutableList<T>).add(created)

            if(numGenerated >= total) break
        }
    }

//    private fun doFinalize(): Sequence<Component> = sequence {
//        log(env.now, env.curComponent, this@ComponentGenerator, "till reached")
//    }

    override val snapshot: ComponentGeneratorSnapshot<T>
        get() = ComponentGeneratorSnapshot(this)
}
