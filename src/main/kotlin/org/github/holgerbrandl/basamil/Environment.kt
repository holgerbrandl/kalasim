package org.github.holgerbrandl.basamil

import java.util.*

val MAIN = "main"

class Environment {

    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false
    private var stopped: Boolean = false


    val eventQueue = PriorityQueue<QueueElement>()

    val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0;
    var offset = 0;

    val main = Component(env = this, name = "main")
    private var curComponent: Component? = main

    val standBy = listOf<Component>().toMutableList()
    val pendingStandBy = listOf<Component>().toMutableList()

    fun build(builder: (Environment.() -> Environment)): Environment {
        builder(this)
        return (this);
    }

    fun addComponent(c: Component) = components.add(c)


    /**
     *         start execution of the simulation
     */
    fun run(until: Int = Int.MAX_VALUE) {
        val scheduled_time = until

//        main.reschedule(scheduled_time, priority, urgent, "run", extra=extra)
        main.reschedule(scheduled_time)

        running = true
        while (running) {
            step()
        }
    }

    private fun step() {
        TODO("Not yet implemented")
    }

    fun now(): Int = now - offset


    fun addStandy(component: Component) {
        standBy.add(component)
    }

    fun addPendingStandBy(component: Component) {
        pendingStandBy.add(component)

    }

    fun addTraceListener(tr: TraceListener) = traceListeners.add(tr)
    fun removeTraceListener(tr: TraceListener) = traceListeners.remove(tr)

    /**
     *         prints a trace line
     *
     *   @param component (usually formatted  now), padded to 10 characters
     *  @param action (usually only used for the compoent that gets current), padded to 20 characters
     */
    fun printTrace(time: Int?, component: Component?, action: String, info: String) {
        listOf(time.toString(), component?.name, action, info)
            .map { (it ?: "").padEnd(12) }.joinToString("")
            .let { println(it) }

        traceListeners.forEach {
            it.processTrace(TraceElement(time, component, action, info))
        }
    }
}

data class TraceElement(val time: Int?, val component: Component?, val action: String, val info: String)