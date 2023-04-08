package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.ComponentState.*
import org.kalasim.Defaults.DEFAULT_SEED
import org.kalasim.Priority.Companion.NORMAL
import org.kalasim.analysis.ConsoleTraceLogger
import org.kalasim.analysis.InteractionEvent
import org.kalasim.misc.*
import org.kalasim.monitors.MetricTimeline
import org.koin.core.Koin
import org.koin.core.definition.Definition
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlinx.datetime.Instant
import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit.*

internal const val MAIN = "main"

typealias KoinModule = org.koin.core.module.Module

//internal class EnvBuildContext : KoinModule() {
//    var enableConsoleLogger: Boolean = true
//}
// --> not possible because Module is not open

// https://github.com/InsertKoinIO/koin/issues/801
fun configureEnvironment(
    enableConsoleLogger: Boolean = false, builder: KoinModule.() -> Unit
): Environment = declareDependencies(builder).createSimulation(enableConsoleLogger) {}


fun declareDependencies(
    builder: KoinModule.() -> Unit
): KoinModule = module(createdAtStart = true) { builder() }


fun KoinModule.createSimulation(
    enableConsoleLogger: Boolean = false,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    durationUnit: DurationUnit = MINUTES,
    /** The absolute time at tick-time 0. Defaults to `null`.*/
    startDate: Instant? = null,
    enableTickMetrics: Boolean = false,
    useCustomKoin: Boolean = false,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit
): Environment = createSimulation(
    durationUnit = durationUnit,
    startDate = startDate,
    enableConsoleLogger = enableConsoleLogger,
    enableTickMetrics = enableTickMetrics,
    dependencies = this,
    useCustomKoin = useCustomKoin,
    randomSeed = randomSeed,
    builder = builder
)

fun createSimulation(
    enableConsoleLogger: Boolean = false,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    durationUnit: DurationUnit = MINUTES,
    /** The absolute time at tick-time 0. Defaults to `null`.*/
    startDate: Instant? = null,
    enableTickMetrics: Boolean = false,
    dependencies: KoinModule? = null,
    useCustomKoin: Boolean = false,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit
): Environment = Environment(
    durationUnit = durationUnit,
    startDate = startDate,
    enableConsoleLogger = enableConsoleLogger,
    enableTickMetrics = enableTickMetrics,
    dependencies = dependencies,
    randomSeed = randomSeed,
    koin = if(useCustomKoin) koinApplication { }.koin else null
).apply(builder)


//fun Environment.createSimulation(builder: Environment.() -> Unit) {
//    this.apply(builder)
//}

object Defaults {
    const val DEFAULT_SEED = 42
}


internal class MainComponent(koin: Koin) : Component(MAIN, koin = koin) {
    override fun process() = sequence<Component> {}
}

fun main() {
    val environment = Environment(DAYS)

    environment.run(4.days)
}

/** An environment hosts all elements of a simulation, maintains the event loop, and provides randomization support. For details see  https://www.kalasim.org/basics/#simulation-environment */
open class Environment(
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    val durationUnit: DurationUnit = MINUTES,
    /** The absolute time at tick-time 0. Defaults to `null`.*/
    var startDate: Instant? = null,
    enableConsoleLogger: Boolean = false,
    enableTickMetrics: Boolean = false,
    dependencies: KoinModule? = null,
    koin: Koin? = null,
    randomSeed: Int = DEFAULT_SEED,
    tickTimeOffset: TickTime = TickTime(0.0),
    // see https://github.com/holgerbrandl/kalasim/issues/49
//    val typedDurationsRequired: Boolean = false
) : SimContext, WithJson {

    private var running: Boolean = false

    val rg: RandomGenerator = JDKRandomGenerator(randomSeed)
    val random: kotlin.random.Random = kotlin.random.Random(randomSeed.toLong())


    internal val nameCache = mutableMapOf<String, Int>()

    // As discussed in https://github.com/holgerbrandl/kalasim/issues/8, we could alternatively use a fibonacci
    // heap for better performance
    private val eventQueue = PriorityQueue<QueueElement>()

    /** Unmodifiable sorted view of currently scheduled components. */
    val queue: List<Component>
        //        get() = eventQueue.map { it.component }
        get() = eventQueue.sortedIterator().map { it.component }.toList()

    private val eventListeners = listOf<EventListener>().toMutableList()


    val trackingPolicyFactory = TrackingPolicyFactory()

//    val traceFilters = mutableListOf<EventFilter>()
//
//    init {
//        traceFilters.add(EventFilter {
//            if(it !is InteractionEvent) return@EventFilter true
//
//            val action = it.renderAction()
//
//            !(action.contains("entering requesters")
//                    || action.contains("entering claimers")
//                    || action.contains("removed from requesters")
//                    || action.contains("removed from claimers"))
//        })
//    }

    /** The current time of the simulation. See https://www.kalasim.org/basics/#running-a-simulation.*/
    var now = tickTimeOffset
        private set // no longer needed/wanted --> use run

    @Suppress("LeakingThis")
    // That's a pointless self-recursion, but it allows to simplify support API (tick-transform, distributions, etc.)
    /** Self-pointer. Pointless from a user-perspective, but helpful to build the kalasim APIs more efficiently.*/
    override val env: Environment = this


    /** Allows to transform ticks to wall time (represented by `java.time.Instant`) */
    internal var tickTransform: TickTransform = TickTransform(durationUnit)

    //    var startDate: Instant? = null
    var startTime: Instant? = null
//        get() = if(tickTransform is OffsetTransform) (tickTransform as OffsetTransform) else null


    /** The component of the currently iterated process definition. Read-only, as components enter the queue only
     * indirectly via scheduling interactions such as for example hold(), request() or wait(). */
    var currentComponent: Component? = null
        private set

    internal val main: Component

    @Suppress("PropertyName")
    internal val _koin: Koin

    final override fun getKoin(): Koin = _koin

    //redeclare to simplify imports
    /** Resolves a dependency in the simulation. Dependencies can be disambiguated by using a qualifier.*/
    inline fun <reified T : Any> get(
        qualifier: Qualifier? = null, noinline parameters: ParametersDefinition? = null
    ): T = getKoin().get(qualifier, parameters)


    init {

        // start console logger

//        addTraceListener { print(it) }
        if(enableConsoleLogger) {
            enableConsoleLogger()
        }

        _koin = koin ?: run {
//            DependencyContext.stopKoin()

            //https://medium.com/koin-developers/ready-for-koin-2-0-2722ab59cac3

            // https://github.com/InsertKoinIO/koin/issues/972
//        CustomContext.startKoin(koinContext = CustomContext()) { modules(module { single { this@Environment } }) }
            DependencyContext.startKoin()
        }


//        require(koins.createAtStart) {
//            "createAtStart must be enabled by convention to instantiate injected components before starting the simulation"
//        }

        getKoin().loadModules(listOf(module {
            single {
                this@Environment
            }
        }))


        main = MainComponent(getKoin())

        // declare dependencies
        if(dependencies != null) {
//            val deps = dependencies ?: (module(createdAtStart = true) { })
            getKoin().loadModules(listOf(dependencies))
//        KoinContextHandler.get()._scopeRegistry.rootScope.createEagerInstances()
//        startKoin { modules(koins) }
        }

//        curComponent = main
    }

    fun enableConsoleLogger() {
        addEventListener(ConsoleTraceLogger())
    }

    private val _tm: TickMetrics? = if(enableTickMetrics) TickMetrics(koin = koin) else null

    val tickMetrics: MetricTimeline<Int>
        get() {
            require(_tm != null) { "Use enableTickMetrics=true to enable tick metrics" }
            return _tm.timeline
        }

//    private var endOnEmptyEventlist = false

    private val standBy = mutableListOf<Component>()
    private val pendingStandBy = mutableListOf<Component>()

//    fun build(vararg compoennts: Component) = components.forEach { this + it }

    // seesm unused. To be deleted in v0.9
//    fun build(builder: (Environment.() -> Unit)): Environment {
//        builder(this)
//        return (this)
//    }


    /**
     * Start execution of the simulation. See https://www.kalasim.org/basics/#running-a-simulation
     *
     * @param duration Time to run. Requires tick-transform to be configured.
     * @param priority If a component has the same time on the event list, the main component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    @OptIn(AmbiguousDuration::class)
    fun run(
        duration: Duration? = null, until:Instant? = null, priority: Priority = NORMAL
    ) = run(duration?.asTicks(), until?.toTickTime(), priority)

//    /**
//     * Start execution of the simulation. See https://www.kalasim.org/basics/#running-a-simulation
//     *
//     * @param duration Time to run
//     * @param priority If a component has the same time on the event list, the main component is sorted according to
//     * the priority. An event with a higher priority will be scheduled first.
//     */
//    fun run(
//        duration: Ticks? = null,
//        priority: Priority = NORMAL
//    ) = run(duration?.value, null, priority)

    /**
     * Start execution of the simulation
     *
     * @param until Absolute time until the which the simulation should run. Requires tick-transform to be configured.
     * @param priority If a component has the same time on the event list, the main component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    @OptIn(AmbiguousDuration::class)
    fun run(
        until: Instant, priority: Priority = NORMAL
    ) = run(until = until.toTickTime(), priority = priority)

    /**
     * Start execution of the simulation
     *
     * If neither `until` nor `ticks` are specified, the main component will be reactivated at
     * the time there are no more events on the event-list, i.e. possibly not at Double.MAX_VALUE. If you want
     * to keep a simulation running simply call `run(Double.MAX_VALUE)`.
     *
     * @param duration Time to run
     * @param until Absolute tick-time until the which the simulation should run
     * @param priority If a component has the same time on the event list, the main component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    @AmbiguousDuration
    fun run(
        duration: Number? = null, until: TickTime? = null, priority: Priority = NORMAL, urgent: Boolean = false
    ) {
        // also see https://simpy.readthedocs.io/en/latest/topical_guides/environments.html
        if(duration == null && until == null) {
//            endOnEmptyEventlist = true
        } else {
            val scheduledTime = calcScheduleTime(until, duration)

            main.reschedule(scheduledTime, priority, urgent, "running", ScheduledType.HOLD)
        }

        // restore dependency context
        DependencyContext.setKoin(_koin)

        running = true

        while(running) {
            step()
        }
    }

    /** Executes the next step of the future event list. */
    private fun step() {

        pendingStandBy.removeIf { it.componentState != STANDBY }

        pendingStandBy.removeFirstOrNull()?.let {
            setCurrent(it) // , "standby" --> removed field in  v0.8
            it.callProcess()
            return
        }

        // move previously standby to pending-standby
        pendingStandBy += standBy
        standBy.clear()


        val (time, component) = if(eventQueue.isNotEmpty()) {
            val (c, time, _, _) = eventQueue.poll()

            time to c
        } else {
            publishEvent(InteractionEvent(now, currentComponent, null, "run end; no events left"))
            val t =
//                if (endOnEmptyEventlist) {
//                publishEvent(InteractionEvent(now, curComponent, null, null, "run end; no events left"))
                now
//            } else {
//                TickTime(Double.MAX_VALUE)
//            }

            t to main
        }

        require(time >= now) { "clock must not run backwards" }

        now = time

        setCurrent(component)

        if(component == main) {
            running = false
            return
        }

        component.checkFail()
        component.callProcess()
    }

    private fun setCurrent(c: Component) {
        c.componentState = CURRENT
        c.scheduledTime = null

        currentComponent = c
//        c.log(c, info)
    }


    //
    // Events
    //


    inline fun <reified T : Event> addAsyncEventListener(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default), crossinline block: (T) -> Unit
    ) = AsyncEventListener(scope).also { listener ->
        listener.start(block)
        addEventListener(listener)
    }

    inline fun <reified T : Event> addEventListener(
        crossinline block: (T) -> Unit
    ) = addEventListener listener@{
        if(it !is T) return@listener
        block(it)
    }

    fun addEventListener(listener: EventListener) = eventListeners.add(listener)

    @Suppress("unused")
    fun removeEventListener(tr: EventListener) = eventListeners.remove(tr)


    internal fun publishEvent(event: Event) {
        eventListeners.forEach {
            it.consume(event)
        }
    }

    /**
     * Stops the simulation after the current event step. This will preserve its queue and process state.
     *
     * See https://www.kalasim.org/basics/#running-a-simulation).
     */
    fun stopSimulation() {
        main.activate()
    }


    //
    // Misc
    //


    internal fun addStandBy(component: Component) {
        standBy.add(component)
    }


    /** Remove a component from the event-queue. Also, remove it from standing-by list, if currently on stand-by.*/
    fun remove(c: Component) {
        unschedule(c)

        // TODO what is happening here, can we simplify that?
        if(c.componentState == STANDBY) {
            standBy.remove(c)
            pendingStandBy.remove(c)
        }
    }

    internal fun unschedule(c: Component) {
        val queueElem = eventQueue.firstOrNull {
            it.component == c
        }

        if(queueElem != null) {
            eventQueue.remove(queueElem)
        }
    }

    private var queueCounter: Int = 0

    internal fun push(component: Component, scheduledTime: TickTime, priority: Priority, urgent: Boolean) {
        queueCounter++

//        https://bezkoder.com/kotlin-priority-queue/
        // Remove an element from the Priority Queue => Dequeue the least element. The front of the Priority Queue
        // contains the least element according to the ordering, and the rear contains the greatest element.
        eventQueue.add(QueueElement(component, scheduledTime, Priority(-priority.value), queueCounter, urgent))

        // consistency checks
        if(ASSERT_MODE == AssertMode.FULL) {
            val scheduledComponents = queue

            require(scheduledComponents.none(Component::isPassive)) { "passive component must not be in event queue" }

            // ensure that no scheduled components have the same name
            require(scheduledComponents.map { it.name }.distinct().size == scheduledComponents.size) {
                "components must not have the same name"
            }
        }
    }

    override fun toJson() = json {
        "now" to now
        "queue" to queue.toList().map { it.name }.toTypedArray()
    }

    fun hasAbsoluteTime() = startDate != null


    fun tick2wallTime(tickTime: TickTime): Instant {
        return startDate!! + tickTransform.ticks2Duration(tickTime.value)
    }

    fun wall2TickTime(instant: Instant): TickTime {
        val offsetDuration = instant - startDate!!

        return TickTime(tickTransform.durationAsTicks(offsetDuration))
    }


    // deprecated because nothing should be logged outside a process
//    fun log(msg: String) = main.log(msg)
}


data class QueueElement(
    val component: Component, val time: TickTime, val priority: Priority, val queueCounter: Int, val urgent: Boolean
) : Comparable<QueueElement> {
    //TODO clarify if we need/want to also support urgent

    override fun compareTo(other: QueueElement): Int =
        compareValuesBy(this, other, { it.time.value }, { it.priority.value }, { it.queueCounter })

//    val heapSeq = if (urgent) -queueCounter else queueCounter

    override fun toString(): String {
//        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $queueCounter) : ${component.componentState}"
    }
}


internal fun Environment.calcScheduleTime(until: TickTime?, duration: Number?): TickTime {
    return (until?.value to duration?.toDouble()).let { (till, duration) ->
        if(till == null) {
            require(duration != null) { "neither duration nor till specified" }
            now.value + duration
        } else {
            require(duration == null) { "both duration and till specified" }
            till
        }
    }.let { TickTime(it) }
}


inline fun <reified T> KoinModule.add(
    qualifier: Qualifier? = null, noinline definition: Definition<T>
) {
    single(qualifier = qualifier, createdAtStart = true, definition = definition)

}

inline fun <reified T> Environment.dependency(qualifier: Qualifier? = null, builder: Environment.() -> T): T {
    val something = builder(this)
    getKoin().loadModules(listOf(module(createdAtStart = true) {
        add(qualifier) { something }
    }))

    return something
}


