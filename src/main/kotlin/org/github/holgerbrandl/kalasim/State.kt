package org.github.holgerbrandl.kalasim

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * States together with the Component.wait() method provide a powerful way of process interaction.

A state will have a certain value at a given time. In its simplest form a component can then wait for a specific value of a state. Once that value is reached, the component will be resumed.

Definition is simple, like dooropen=sim.State('dooropen'). The default initial value is False, meaning the door is closed.
 * initial value of the state
 */
class State<T>(initialValue: T, name: String? = null) : SimulationEntity(name) {

    var value: T = initialValue
        set(value) {
            if (field == value) return

            field = value

            // todo ensure that this is called also for the initial value
            valueMonitor.addValue(value)
            env.printTrace("set value to ${value}")
            tryWait()
        }



    private val valueMonitor = FrequencyLevelMonitor<T>()

    internal val waiters = ComponentQueue<Component>("waiters of ${this.name}")
//    val waiters = PriorityQueue<Component>()

    override fun toString(): String = super.toString() + "[${value}]"

    fun trigger(max: Int, value:T, valueAfter: T?=null) {
//        waiters.
        TODO("Not yet implemented")

    }

    // conf example
    private fun tryWait(max: Int = Int.MAX_VALUE) {
        waiters.q.map { it.c }.take(max).forEach { it.tryWait() }
    }

    public override val info
        get() = StateInfo(env.now, name, value.toString(), waiters.q.map { it.c.name })

}


/** Captures the current state of a `State`*/
@Serializable
data class StateInfo(val time: Double, val name: String, val value: String, val waiters: List<String>) : Snapshot() {
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}

fun main() {
    State("foo").printInfo()
}