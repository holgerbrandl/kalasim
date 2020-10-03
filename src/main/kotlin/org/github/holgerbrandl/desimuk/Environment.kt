package org.github.holgerbrandl.desimuk

import org.github.holgerbrandl.desimuk.State.CURRENT
import org.github.holgerbrandl.desimuk.State.STANDBY
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import java.text.DecimalFormat
import java.util.*


val MAIN = "main"

open class Environment(koins: org.koin.core.module.Module = module { }) : KoinComponent {

    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false
    private var stopped: Boolean = false


    val eventQueue = PriorityQueue<QueueElement>()

    val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0.0
        private set

    private var curComponent: Component?

    var main: Component
        private set
    init {
        startKoin { modules(module { single { this@Environment } }, koins) }

        main = Component(name = "main", process = null)
        curComponent = main

    }


    var endOnEmptyEventlist = false

    val standBy = listOf<Component>().toMutableList()
    val pendingStandBy = listOf<Component>().toMutableList()


//    fun build(vararg compoennts: Component) = compoennts.forEach { this + it }

    fun build(builder: (Environment.() -> Unit)): Environment {
        builder(this)
        return (this)
    }

    fun addComponent(c: Component): Boolean {
        require(!components.contains(c)) { "we must not add a component twice" }
        return components.add(c)
    }


    /**
     * Start execution of the simulation
     */
    fun run(duration: Double? = null, until: Double? = null, priority: Int = 0, urgent: Boolean = false): Environment {

        if (duration == null) endOnEmptyEventlist = true


        val scheduled_time = calcScheduleTime(until, duration)

        main.reschedule(scheduled_time, priority, urgent, "run", extra = "TODO implement me")

        running = true
        while (running) {
            step()
        }

        return (this)
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

                    printTrace(now, it, "current (standby)")

                    it.callProcess()
                }

        }

        pendingStandBy += standBy
        standBy.clear()


        val (time, component) = if (eventQueue.isNotEmpty()) {
            val (time, _, _, c) = eventQueue.poll()

            time to c
        } else {
            val t = if (endOnEmptyEventlist) {
                printTrace(now, null, "run ended", "no events left")
                now
            } else {
                Double.MAX_VALUE
            }

            t to main
        }

        require(time >= now) { "clock must not run backwards" }

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


    fun addStandy(component: Component) {
        standBy.add(component)
    }

    fun addPendingStandBy(component: Component) {
        pendingStandBy.add(component)

    }

    fun addTraceListener(tr: TraceListener) = traceListeners.add(tr)

    @Suppress("unused")
    fun removeTraceListener(tr: TraceListener) = traceListeners.remove(tr)


    /**
     *         prints a trace line
     *
     *   @param component (usually formatted  now), padded to 10 characters
     *  @param action (usually only used for the compoent that gets current), padded to 20 characters
     */
    fun printTrace(time: Double, component: Component?, action: String, info: String? = null) {
        val tr = TraceElement(time, component, action, info)
        println(tr)

        traceListeners.forEach {
            it.processTrace(tr)
        }
    }

    operator fun <T : Component> plus(componentGenerator: ComponentGenerator<T>): Environment {
        addComponent(componentGenerator); return (this)
    }

    operator fun plus(component: Component): Environment {
        addComponent(component); return (this)
    }
}

private val DF = DecimalFormat("#.00")


data class TraceElement(val time: Double, val component: Component?, val action: String, val info: String?) {
    override fun toString(): String {
        return listOf(DF.format(time).padStart(7), component?.name, component?.name + " " + action, info)
            .map { (it ?: "") }
            .zip(listOf(10, 25, 30, 30))
            .map { (str, padLength) -> str.padEnd(padLength) }
            .joinToString("")
    }
}

fun Environment.calcScheduleTime(till: Double?, duration: Double?): Double {
    return if (till == null) {
        if (duration == null) now else {
            now + duration
        }
    } else {
        require(duration == null) { "both duration and till specified" }
        till
    }
}


inline fun <reified T> org.koin.core.module.Module.add(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>
) {
    single(qualifier = qualifier, createdAtStart = true, definition = definition)

}
