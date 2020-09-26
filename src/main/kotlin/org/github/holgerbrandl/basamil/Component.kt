package org.github.holgerbrandl.basamil

import kotlin.reflect.KFunction1


/**
 * A salabim component is used as component (primarily for queueing)
or as a component with a process
Usually, a component will be defined as a subclass of Component.
 *
 * @param process  of process to be started.  if None (default), it will try to start self.process()
 * @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)

 */
open class Component(
    val env: Environment,
    val name: String?,
    val process: KFunction1<Component, Sequence<Any>> = Component::process,
) {

    private var offset = 0;
    private var now = 0;
    private var scheduled_time = Int.MAX_VALUE


    /**         the current simulation time : float */
    fun now() = now - offset

    open fun process(): Sequence<Any> = sequence { }

    // todo
    fun hold(until: Int): Component = this

    fun reschedule(
        scheduledTime: Int,
        priority: Int = 0,
        urgent: Boolean = false,
        caller: String? = null,
        extra: Any? = null
    ) {
        require(scheduledTime <= env.now()) { "scheduled time ({:0.3f}) before now ({:0.3f})" }

        this.scheduled_time = scheduledTime

        if (scheduled_time != Int.MAX_VALUE) push(scheduled_time, priority, urgent)

        //todo implement extra
        val extra = ""

        // calculate scheduling delta
        val delta = if (scheduled_time == env.now() || (scheduled_time == Int.MAX_VALUE)) "" else {
            "+" + (scheduled_time - env.now())
        }

        // print trace
        env.printTrace("", "", name + " " + caller + " " + delta, extra)
    }

    var seq: Int = 0

    private fun push(scheduledTime: Int, priority: Int, urgent: Boolean) {
        seq++
        val heapSeq = if (urgent) -seq else seq

//        https://bezkoder.com/kotlin-priority-queue/
        env.eventQueue.add(QueueElement(scheduledTime, priority, seq, this))
    }
}

data class QueueElement(val time: Int, val priority: Int, val seq: Int, val component: Component)

enum class State {
    data, current, standyby, passive, interrupted, scheduled, requesting, waiting
}
