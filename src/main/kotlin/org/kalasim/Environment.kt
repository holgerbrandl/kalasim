@file:Suppress("EXPERIMENTAL_API_USAGE")

package org.kalasim

import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.ComponentState.*
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.*


const val MAIN = "main"

typealias CompRegistry = Koin

//internal class EnvBuildContext : org.koin.core.module.Module() {
//    var enableTraceLogger: Boolean = true
//}
// --> not possible because Module is not open

// https://github.com/InsertKoinIO/koin/issues/801
fun configureEnvironment(
    enableTraceLogger: Boolean = false,
    builder: org.koin.core.module.Module.() -> Unit
): Environment =
    Environment(module(createdAtStart = true) { builder() }, enableTraceLogger)


fun createSimulation(
    enableTraceLogger: Boolean = false,
    useCustomKoin: Boolean = false,
    builder: Environment.() -> Unit
): Environment =
    Environment(
        enableTraceLogger = enableTraceLogger,
        koin = if (useCustomKoin) koinApplication { }.koin else null
    ).apply(builder)

//fun Environment.createSimulation(builder: Environment.() -> Unit) {
//    this.apply(builder)
//}

class Environment(
    koins: org.koin.core.module.Module = module(createdAtStart = true) { },
    enableTraceLogger: Boolean = false,
    koin: Koin? = null
) : KoinComponent {

    @Deprecated("serves no purposes and creates a memory leaks as objects are nowhere releases")
    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false

    val rg: RandomGenerator = JDKRandomGenerator(42)

    internal val nameCache = mapOf<String, Int>().toMutableMap()

    private val eventQueue = PriorityQueue<QueueElement>()

    /** Unmodifiable view on `eventQueue`. */
    val queue: List<Component>
        get() = eventQueue.map { it.component }


    private val traceListeners = listOf<TraceListener>().toMutableList()

    var now = 0.0
        internal set

    var curComponent: Component? = null
        private set

    var main: Component
        private set

    val _koin: Koin

    override fun getKoin(): Koin = _koin

    init {

        // start console logger

//        addTraceListener { print(it) }
        if (enableTraceLogger) {
            addTraceListener(ConsoleTraceLogger(true))
        }

        _koin =  koin ?: run{
            GlobalContext.stop()

            //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3

            // https://github.com/InsertKoinIO/koin/issues/972
//        CustomContext.startKoin(koinContext = CustomContext()) { modules(module { single { this@Environment } }) }
            startKoin() {
            }.koin
        }


//        require(koins.createAtStart) {
//            "createAtStart must be enabled by convention to instantiate injected components before starting the simulation"
//        }

        getKoin().loadModules(listOf(module {
            single {
                this@Environment
            }
        }), createEagerInstances = true)


        main = Component(name = "main", process = null, koin = getKoin())
        setCurrent(main)

        getKoin().loadModules(listOf(koins), createEagerInstances = true)
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
    fun run(duration: Number? = null, until: Number? = null, priority: Int = 0, urgent: Boolean = false): Environment {
        // TODO https://simpy.readthedocs.io/en/latest/topical_guides/environments.html
        //  If you call it without any argument (env.run()), it steps through the simulation until there are
        //  no more events left.

        // TODO add test coverage for `until`

        if (duration == null) endOnEmptyEventlist = true

        val scheduledTime = calcScheduleTime(until, duration)

        main.reschedule(scheduledTime, priority, urgent, "run", SCHEDULED)

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

        c.printTrace(c, info)
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

    private var queueCounter: Int = 0

    fun push(component: Component, scheduledTime: Double, priority: Int, urgent: Boolean) {
        queueCounter++

//        https://bezkoder.com/kotlin-priority-queue/
        // Remove an element from the Priority Queue => Dequeue the least element. The front of the Priority Queue
        // contains the least element according to the ordering, and the rear contains the greatest element.
        eventQueue.add(QueueElement(component, scheduledTime, -priority, queueCounter, urgent))

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
    //TODO clarify if we need/want to also support urgent

    override fun compareTo(other: QueueElement): Int =
        compareValuesBy(this, other, { it.time }, { it.priority }, { it.queueCounter })

    val heapSeq = if (urgent) -queueCounter else queueCounter


    override fun toString(): String {
//        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $queueCounter) : ${component.status}"
    }
}


fun Environment.calcScheduleTime(till: Number?, duration: Number?): Double {
    return (till?.toDouble() to duration?.toDouble()).let { (till, duration) ->
        if (till == null) {
            if (duration == null) now else {
                now + duration
            }
        } else {
            require(duration == null) { "both duration and till specified" }
            till
        }
    }
}


inline fun <reified T> org.koin.core.module.Module.add(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>
) {
    single(qualifier = qualifier, createdAtStart = true, definition = definition)

}
