package org.github.holgerbrandl.desim

import org.github.holgerbrandl.desim.State.CURRENT
import org.github.holgerbrandl.desim.State.STANDBY
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import java.util.*


val MAIN = "main"


class Environment(koins: org.koin.core.module.Module = module { }) : KoinComponent {

    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false
    private var stopped: Boolean = false


    val eventQueue = PriorityQueue<QueueElement>()

    val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0.0
        private set

    var curComponent: Component? = null
        private set

    var main: Component
        private set

    init {

        // start console logger

//        addTraceListener { print(it) }
        addTraceListener(ConsoleTraceLogger(true))

        startKoin { modules(module { single { this@Environment } }) }

        main = Component(name = "main", process = null)
        setCurrent(main)

        require(koins.createAtStart) {
            "createAtStart must be enabled by convention to instantiate injected components before starting the simulation"
        }

        getKoin().loadModules(listOf(koins))
//        startKoin { modules(koins) }

//        curComponent = main

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
                    setCurrent(it, "standby")
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
                printTrace(now, curComponent, null, "run end; no events left")
                now
            } else {
                Double.MAX_VALUE
            }

            t to main
        }

        require(time >= now) { "clock must not run backwards" }

        now = time

        setCurrent(component)

        if (component == main) {
            running = false
            return
        }

        component.callProcess()
    }

    private fun setCurrent(c: Component, info: String? = null) {
        c.status = CURRENT
        c.scheduledTime = Double.MAX_VALUE

        curComponent = c

        printTrace(now, curComponent, c, info)
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


    fun printTrace(info: String) = printTrace(now, curComponent, null, info)

    fun <T : Component> printTrace(element: T, info: String) = printTrace(now, curComponent, element, info)


    /**
     *         prints a trace line
     *
     *   @param component (usually formatted  now), padded to 10 characters
     *  @param state (usually only used for the compoent that gets current), padded to 20 characters
     */
    fun printTrace(
        time: Double,
        curComponent: Component?,
        component: Component?,
        info: String? = null
    ) {
        val tr = TraceElement(time, curComponent, component, info)

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
