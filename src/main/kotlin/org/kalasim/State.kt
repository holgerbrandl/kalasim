package org.kalasim

import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.analysis.StateChangedEvent
import org.kalasim.analysis.snapshot.StateSnapshot
import org.kalasim.misc.DependencyContext
import org.kalasim.misc.StateTrackingConfig
import org.kalasim.monitors.*
import org.koin.core.Koin

/**
 * States together with the Component.wait() method provide a powerful way of process interaction.
 *
 * A state will have a certain value at a given time. In its simplest form a component can then wait for a specific value of a state. Once that value is reached, the component will be resumed.
 *
 * @sample org.kalasim.dokka.statesHowTo
 */
open class State<T>(
    initialValue: T,
    name: String? = null,
    koin: Koin = DependencyContext.get(),
    val trackingConfig: StateTrackingConfig = koin.getEnvDefaults().DefaultStateConfig,
) : SimulationEntity(name, koin) {

    private var maxTriggerCxt: Int? = null
    private val isTriggerCxt
        get() = maxTriggerCxt != null

    var value: T = initialValue
        set(value) {


            if(field == value) return

            field = value

            // salabim also logs the set even if the state.value may not change
            log(trackingConfig.logTriggers) {
                StateChangedEvent(
                    env.now,
                    this,
                    value,
                    current = env.currentComponent,
                    if(isTriggerCxt) maxTriggerCxt else null,
                )
            }

            changeListeners.forEach { it.stateChanged(this) }

            timeline.addValue(value)

//            if (Thread.currentThread().getStackTrace()[2].methodName != "trigger") {
            if(!isTriggerCxt) {
                tryWait()
            }
        }


    val timeline = CategoryTimeline(initialValue = value, koin = koin, name = "State of '$name'")

    internal val waiters = ComponentQueue<Component>("waiters of ${this.name}", koin = koin)
//    val waiters = PriorityQueue<Component>()

    /** Tracks the queue length level along time. */
    val queueLength: MetricTimeline<Int>
        get() = waiters.sizeTimeline

    /** Tracks the length of stay in the queue over time*/
    val lengthOfStay: NumericStatisticMonitor
        get() = waiters.lengthOfStayStatistics

    init {
        with(trackingConfig) {
            lengthOfStay.enabled = trackQueueStatistics
            queueLength.enabled = trackQueueStatistics

            timeline.enabled = trackValue
        }

        log(trackingConfig.logCreation) {
            EntityCreatedEvent(now, env.currentComponent, this, "Initial value: $value")
        }
    }

    override fun toString(): String = super.toString() + "[${value}]"

    private var isTrigger: Boolean = false

    /**
     * Sets the value `value` and triggers any components waiting,
     * then at most max waiting components for this state  will be honored and next
     * the value will be set to value_after and again checked for possible honors.
     *
     * @param value the new value
     * @param valueAfter After the trigger this will be the new value. If omitted, return to the value before the trigger
     * @param max Maximum number of components to be honored for the trigger value
     */
    fun trigger(value: T, valueAfter: T = this.value, max: Int = Int.MAX_VALUE) {
        triggerContext(max) {
            this.value = value
        }

        tryWait(max)

        // restore to valueAfter
        this.value = valueAfter
    }

    private fun triggerContext(max: Int, smthg: () -> Unit) {
        maxTriggerCxt = max
        smthg()
        maxTriggerCxt = null
    }

    //    private fun tryWait(maxHonor: Int = Int.MAX_VALUE) {
//        var mx = maxHonor
//        waiters.q.map { it.component }.takeWhile {
//            // wait max times but consider honor return state of tryWait
//            if(it.tryWait()) mx--
//            mx > 0
//        }
//    }
    // throws concurrent modification exception
//    private fun tryWait(maxHonor: Int = Int.MAX_VALUE) {
//        var remainingHonor = maxHonor
//        val iterator = waiters.q.iterator()
//
//        while (remainingHonor > 0 && iterator.hasNext()) {
//            val waiter = iterator.next().component
//            if (waiter.tryWait()) {
//                remainingHonor--
//            }
//        }
//    }
    private fun tryWait(maxHonor: Int = Int.MAX_VALUE) {
        val copyOfQ = ArrayList(waiters.q) // Make a copy of the collection
        var remainingHonor = maxHonor

        for(waiter in copyOfQ) {
            val component = waiter.component
            if(remainingHonor <= 0) {
                break
            }
            if(component.tryWait()) {
                remainingHonor--
            }
        }
    }


    fun printHistograms() {
        waiters.printHistogram()
        timeline.printHistogram()
    }

    internal val changeListeners = mutableListOf<StateChangeListener<T>>()

    fun interface StateChangeListener<T> {
        fun stateChanged(state: State<T>)
    }

    /** Register a change listener. Will be invoked on every value change of the state. */
    fun onChange(function: StateChangeListener<T>) = changeListeners.add(function)

    override val snapshot
        get() = StateSnapshot(env.now, name, value.toString(), waiters.q.map { it.component.name })
}
