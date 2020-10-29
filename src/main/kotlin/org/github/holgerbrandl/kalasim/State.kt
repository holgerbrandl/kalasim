package org.github.holgerbrandl.kalasim

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * States together with the Component.wait() method provide a powerful way of process interaction.

A state will have a certain value at a given time. In its simplest form a component can then wait for a specific value of a state. Once that value is reached, the component will be resumed.

Definition is simple, like dooropen=sim.State('dooropen'). The default initial value is False, meaning the door is closed.
 * initial value of the state
 */
class State<T>(initialValue: T, name: String? = null) : SimulationEntity(name) {

    var value = initialValue

    val waiters = ComponentQueue<Component>("waiters of ${name}")
//    val waiters = PriorityQueue<Component>()

    fun addWaiter(component: Component, priority: Int?) =
        waiters.add(component, priority)

    fun removeWaiter(component: Component) =
        waiters.remove(component)

    fun trigger(max: Int) {
//        waiters.
        TODO("Not yet implemented")

    }

    override val info
        get() = StateInfo(env.now, name, value.toString(), waiters.q.map { it.c.name })

}


/** Captures the current state of a `State`*/
@Serializable
data class StateInfo(val time: Double, val name: String, val value: String, val waiters: List<String>) : Snapshot(){
    override fun toString(): String {
        return Json.encodeToString(this)
    }
}

fun main() {
    State("foo").printInfo()
}