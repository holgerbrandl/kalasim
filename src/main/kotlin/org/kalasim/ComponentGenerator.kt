package org.kalasim

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.analysis.snapshot.ComponentGeneratorSnapshot
import org.kalasim.misc.AmbiguousDuration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A component generator can be used to generate components.
 *
 * For supported arguments see https://www.kalasim.org/component/#component-generator
 */
class ComponentGenerator<T>(
    val iat: DurationDistribution,
    startAt: SimTime? = null,
    val forceStart: Boolean = false,
    val until: SimTime? = null ,
    val total: Int = Int.MAX_VALUE,
    name: String? = null,
    priority: Priority = Priority.NORMAL,
    val keepHistory: Boolean = false,
    envProvider: EnvProvider = DefaultProvider(),
    val builder: Environment.(counter: Int) -> T
) : Component(name, priority = priority, at = startAt, process = ComponentGenerator<T>::doIat, envProvider = envProvider) {


    /**
     * A component generator can be used to generate components.
     *
     * For supported arguments see https://www.kalasim.org/component/#component-generator
     */
    @AmbiguousDuration
    @Deprecated("used constructor with DurationDistribution iat parameter instead")
    constructor(
        iat: RealDistribution,
        startAt: SimTime? = null,
        forceStart: Boolean = false,
        until: SimTime? = null,
        total: Int = Int.MAX_VALUE,
        name: String? = null,
        priority: Priority = Priority.NORMAL,
        keepHistory: Boolean = false,
        envProvider: EnvProvider = DefaultProvider(),
        builder: Environment.(counter: Int) -> T
    ) : this(
        DurationDistribution(envProvider.getEnv().tickDurationUnit, iat),
        startAt,
        forceStart,
        until,
        total,
        name,
        priority,
        keepHistory,
        envProvider,
        builder
    )

    /**
     * A component generator can be used to generate components.
     *
     * For supported arguments see https://www.kalasim.org/component/#component-generator
     */
    constructor(
        iat: Duration,
        startAt: SimTime? = null,
        forceStart: Boolean = false,
        until: SimTime? = null,
        total: Int = Int.MAX_VALUE,
        name: String? = null,
        priority: Priority = Priority.NORMAL,
        keepHistory: Boolean = false,
        envProvider: EnvProvider = DefaultProvider(),
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
        envProvider,
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

            if(until!=null && (env.now + interArrivalTime) > until || isData) {
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
