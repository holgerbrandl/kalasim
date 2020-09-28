package org.github.holgerbrandl.basamil

import org.github.holgerbrandl.basamil.State.CURRENT
import org.github.holgerbrandl.basamil.State.STANDBY
import java.util.*

private val Component.isGenerator: Boolean
    get() {
        TODO("Not yet implemented")
    }

val MAIN = "main"

class Environment {

    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false
    private var stopped: Boolean = false


    val eventQueue = PriorityQueue<QueueElement>()

    val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0.0
        private set

    var offset = 0;

    val main = Component(env = this, name = "main", process = null)
    private var curComponent: Component? = main

    var endOnEmptyEventlist = false

    val standBy = listOf<Component>().toMutableList()
    val pendingStandBy = listOf<Component>().toMutableList()

    init {
        main.status = CURRENT
        printTrace(now, main, CURRENT.toString(), null)
    }

    fun build(builder: (Environment.() -> Unit )): Environment {
        builder(this)
        return (this);
    }

    fun addComponent(c: Component) = components.add(c)


    /**
     * Start execution of the simulation
     */
    fun run(duration: Double? = null, until: Double? = null, priority: Int = 0, urgent: Boolean = false) {

        if (duration == null) endOnEmptyEventlist = true


        val scheduled_time = calcScheduleTime(until, duration)

        main.reschedule(scheduled_time, priority, urgent, "run", extra = "TODO implement me")

        running = true
        while (running) {
            step()
        }
    }

    /** Executes the next step of the future event list */
    private fun step() {

        if (pendingStandBy.isNotEmpty()) {
            val notCancelled = pendingStandBy
                // skip cancelled components
                .filter { it.status == STANDBY }

            pendingStandBy.clear()

            notCancelled
                .forEach {
                    it.status = CURRENT
                    it.scheduledTime = Double.MAX_VALUE
                    curComponent = it

                    printTrace(now(), it, "current (standby)")

                    it.callProcess()
                }

        }

        pendingStandBy += standBy
        standBy.clear()


        val (time, component) = if (eventQueue.isNotEmpty()) {
            val (time, priority, seq, c) = eventQueue.poll()

            time to c
        } else {
            val t = if (endOnEmptyEventlist) {
                printTrace(null, null, "run ended", "no events left")
                now
            } else {
                Double.MAX_VALUE
            }

            t to main
        }

        now = time
        curComponent = component
        component.status = CURRENT
        component.scheduledTime = Double.MAX_VALUE

        if (component == main) {
            running = false
            return
        }

        component.callProcess()
    }


    fun now(): Double = now - offset


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
    fun printTrace(time: Double?, component: Component?, action: String, info: String? = null) {
        val tr = TraceElement(time, component, action, info)
        println(tr)

        traceListeners.forEach {
            it.processTrace(tr)
        }
    }

    operator fun <T: Component> plus(componentGenerator: ComponentGenerator<T>)  = addComponent(componentGenerator)
    operator fun  plus(componentGenerator: Component)  = addComponent(componentGenerator)
}

data class TraceElement(val time: Double?, val component: Component?, val action: String, val info: String?) {
    override fun toString(): String {
        return listOf(time.toString(), component?.name, component?.name + " " + action, info)
            .map { (it ?: "") }
            .zip(listOf(5, 14, 20, 20))
            .map { (str, padLength) -> str.padEnd(padLength) }
            .joinToString("")
    }
}

fun Environment.calcScheduleTime(till: Double?, duration: Double?): Double {
    return if (till == null) {
        if (duration == null) now() else {
            now() + duration
        }
    } else {
        require(duration == null) { "both duration and till specified" }
        till + offset
    }
}
