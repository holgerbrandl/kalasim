package org.github.holgerbrandl.kalasim

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.github.holgerbrandl.kalasim.ComponentState.*
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.module
import java.util.*


const val MAIN = "main"

// https://github.com/InsertKoinIO/koin/issues/801
fun configureEnvironment(builder: org.koin.core.module.Module.() -> Unit): Environment =
    Environment(module(createdAtStart = true) { builder() })


fun createSimulation(builder: Environment.() -> Unit): Environment =
    Environment( ).apply(builder)

fun Environment.createSimulation(builder: Environment.() -> Unit){
    this.apply(builder)
}

class Environment(koins: org.koin.core.module.Module = module(createdAtStart = true) { }) : KoinComponent {

    @Deprecated("serves no purposes and creates a memory leaks as objects are nowhere releases")
    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false

    val rg: RandomGenerator = JDKRandomGenerator(42)

    private val eventQueue = PriorityQueue<QueueElement>()

    /** Unmodifiable view on `eventQueue`. */
    val queue: List<Component>
        get() = eventQueue.map{ it.component }


    private val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0.0
        internal set

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
//        KoinContextHandler.get()._scopeRegistry.rootScope.createEagerInstances()
//        startKoin { modules(koins) }

//        curComponent = main

    }


    private var endOnEmptyEventlist = false

    private val standBy = listOf<Component>().toMutableList()
    private val pendingStandBy = listOf<Component>().toMutableList()


//    fun build(vararg compoennts: Component) = components.forEach { this + it }

    fun build(builder: (Environment.() -> Unit)): Environment {
        builder(this)
        return (this)
    }

    @Deprecated("not really needed and shall be removed")
    fun addComponent(c: Component): Boolean {
        require(!components.contains(c)) { "we must not add a component twice" }
        return components.add(c)
    }


    /**
     * Start execution of the simulation
     */
    fun run(duration: Double? = null, until: Double? = null, priority: Int = 0, urgent: Boolean = false): Environment {
        if (duration == null) endOnEmptyEventlist = true

        val scheduledTime = calcScheduleTime(until, duration)

        main.reschedule(scheduledTime, priority, urgent, "run",SCHEDULED)

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
            val (c, time, _, _) = eventQueue.poll()

            time to c
        } else {
            val t = if (endOnEmptyEventlist) {
                publishTraceRecord(TraceElement(now, curComponent, null, null, "run end; no events left"))
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

        c.printTrace(c,info)
    }


    fun addStandBy(component: Component) {
        standBy.add(component)
    }

    fun addPendingStandBy(component: Component) {
        pendingStandBy.add(component)

    }

    fun addTraceListener(tr: TraceListener) = traceListeners.add(tr)

    @Suppress("unused")
    fun removeTraceListener(tr: TraceListener) = traceListeners.remove(tr)


    internal fun publishTraceRecord(tr: TraceElement) {
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

    fun remove(c: Component) {
        unschedule(c)

        // TODO what is happening here, can we simplify that?
        if (c.status == STANDBY) {
            addStandBy(c)
            addPendingStandBy(c)
        }
    }

    internal fun unschedule(c: Component) {
        val queueElem = eventQueue.firstOrNull {
            it.component == c
        }

        if (queueElem != null) {
            eventQueue.remove(queueElem)
        }
    }

    var queueCounter: Int = 0

    fun push(component: Component, scheduledTime: Double, priority: Int, urgent: Boolean) {
        queueCounter++

//        https://bezkoder.com/kotlin-priority-queue/
        eventQueue.add(QueueElement(component, scheduledTime, priority, queueCounter, urgent))

        // consistency checks
        require(queue.none(Component::isPassive)) { "passive component must not be in event queue" }
    }
}




data class QueueElement(
    val component: Component,
    val time: Double,
    val priority: Int,
    val queueCounter: Int,
    val urgent: Boolean
) :
    Comparable<QueueElement> {
    override fun compareTo(other: QueueElement): Int =
        compareValuesBy(this, other, { it.time }, { it.priority }, { -it.queueCounter })

    val heapSeq = if (urgent) -queueCounter else queueCounter


    override fun toString(): String {
//        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $queueCounter) : ${component.status}"
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
