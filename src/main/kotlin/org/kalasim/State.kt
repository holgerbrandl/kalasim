package org.kalasim

import org.kalasim.misc.Jsonable
import org.koin.core.Koin
import org.koin.core.context.GlobalContext

/**
 * States together with the Component.wait() method provide a powerful way of process interaction.

A state will have a certain value at a given time. In its simplest form a component can then wait for a specific value of a state. Once that value is reached, the component will be resumed.

Definition is simple, like dooropen=sim.State('dooropen'). The default initial value is False, meaning the door is closed.
 * initial value of the state
 */
open class State<T>(
    initialValue: T,
    name: String? = null,
    koin: Koin = GlobalContext.get()
) : SimulationEntity(name, koin) {

    var value: T = initialValue
        set(value) {
            if (field == value) return

            field = value

            // todo ensure that this is called also for the initial value
            valueMonitor.addValue(value)

            if (Thread.currentThread().getStackTrace()[2].methodName != "trigger") {
                tryWait()
            }
        }


    private val valueMonitor = FrequencyLevelMonitor<T>(initialValue = value, koin = koin)

    internal val waiters = ComponentQueue<Component>("waiters of ${this.name}", koin = koin)
//    val waiters = PriorityQueue<Component>()

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
        log(
            env.now,
            env.curComponent,
            this,
            "value = ${value} --> ${valueAfter} allow $max components",
            "trigger"
        )

        this.value = value
        tryWait(max)

        this.value = valueAfter
        tryWait()
    }

    private fun tryWait(maxHonor: Int = Int.MAX_VALUE) {
        var mx = maxHonor
        waiters.q.map { it.component }.takeWhile {
            // wait max times but consider honor return state of tryWait
            if (it.tryWait()) mx--;
            mx > 0
        }
    }

    fun printHistograms() {
        waiters.printHistogram()
        valueMonitor.printHistogram()
    }

    public override val info
        get() = StateInfo(env.now, name, value.toString(), waiters.q.map { it.component.name })
}


/** Captures the current state of a `State`*/
//@Serializable
data class StateInfo(val time: Double, val name: String, val value: String, val waiters: List<String>) : Jsonable() {
//    override fun toString(): String {
//        return Json.encodeToString(this)
//    }
}