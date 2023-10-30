package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.kalasim.ComponentState.CURRENT
import org.kalasim.ComponentState.STANDBY
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
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.MINUTES

internal const val MAIN = "main"

typealias KoinModule = org.koin.core.module.Module

// https://github.com/InsertKoinIO/koin/issues/801
@Deprecated("Use createSimulation() instead", ReplaceWith("declareDependencies(builder).createSimulation {}"))
fun configureEnvironment(
    enableComponentLogger: Boolean = false, builder: KoinModule.() -> Unit,
): Environment = declareDependencies(builder).createSimulation {}


@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Use createSimulation() instead")
fun declareDependencies(
    builder: KoinModule.() -> Unit,
): KoinModule = module(createdAtStart = true) { builder() }


fun KoinModule.createSimulation(
    /** The start time of the simulation model. Defaults to 1970-01-01T00:00:00Z following the convention of kotlin.time.Instant.*/
    startDate: SimTime = SimTime.fromEpochMilliseconds(0),
    useCustomKoin: Boolean = false,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    durationUnit: DurationUnit = MINUTES,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit,
): Environment = createSimulation(
    startDate = startDate,
    dependencies = this,
    useCustomKoin = useCustomKoin,
    tickDurationUnit = durationUnit,
    randomSeed = randomSeed,
    builder = builder
)

fun createSimulation(
    /** The start time of the simulation model. Defaults to 1970-01-01T00:00:00Z following the convention of kotlin.time.Instant.*/
    startDate: SimTime = SimTime.fromEpochMilliseconds(0),
    dependencies: KoinModule? = null,
    useCustomKoin: Boolean = false,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    tickDurationUnit: DurationUnit = MINUTES,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit,
): Environment = Environment(
    startDate = startDate,
    tickDurationUnit = tickDurationUnit,
    dependencies = dependencies,
    koin = if(useCustomKoin) koinApplication { }.koin else null,
    randomSeed = randomSeed
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
    val environment = Environment(tickDurationUnit = DAYS)

    environment.run(4.days)
}

/** An environment hosts all elements of a simulation, maintains the event loop, and provides randomization support. For details see  https://www.kalasim.org/basics/#simulation-environment */
open class Environment(
    /** The start time of the simulation model. Defaults to 1970-01-01T00:00:00Z following the convention of kotlin.time.Instant.*/
    val startDate: SimTime = SimTime.fromEpochMilliseconds(0),
    /** If enabled, it will render a tabular view of recorded  interaction and resource events. */
    enableComponentLogger: Boolean = false,

    /** Measure the compute time per tick as function of time. For details see  https://www.kalasim.org/advanced/#operational-control */
    enableTickMetrics: Boolean = false,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    val tickDurationUnit: DurationUnit = MINUTES,
    dependencies: KoinModule? = null,
    koin: Koin? = null,
    randomSeed: Int = DEFAULT_SEED,
//    tickTimeOffset: TickTime = TickTime(0.0),
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
        get() = eventQueue.sortedIterator()
            .map { it.component }
            .toList()

    // intenionally immutable to avoid checkForCodmodificatio when iterating it (suggested by chat-gpt)
    internal var eventListeners: List<EventListener> = emptyList()


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
    var now = startDate
        private set // no longer needed/wanted --> use run

    @Deprecated("use now instead", ReplaceWith("now"))
    val nowWT: Instant = now
//        get() = now.toWallTime()

    val nowTT: TickTime
        get() = now.toTickTime()


    @Suppress("LeakingThis")
    // That's a pointless self-recursion, but it allows to simplify support API (tick-transform, distributions, etc.)
    /** Self-pointer. Pointless from a user-perspective, but helpful to build the kalasim APIs more efficiently.*/
    override val env: Environment = this


    /** Allows to transform ticks to wall time (represented by `kotlinx.datetime.Instant`) */
    internal var tickTransform: TickTransform = TickTransform(tickDurationUnit)


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
        qualifier: Qualifier? = null, noinline parameters: ParametersDefinition? = null,
    ): T = getKoin().get(qualifier, parameters)

    /**
     * Retrieves an instance of type [T] from from simulation environment or null if not found.
     *
     * @param qualifier The optional qualifier to distinguish between multiple instances of the same type [T]. Default is null.
     * @param parameters The optional parameters definition for resolving the instance. Default is null.
     * @return An instance of type [T] retrieved from simulation environment, or null if not found.
     */
    inline fun <reified T : Any> getOrNull(
        qualifier: Qualifier? = null, noinline parameters: ParametersDefinition? = null,
    ): T? = getKoin().getOrNull(qualifier, parameters)

    /** Resolves a dependency in the simulation. Dependencies can be disambiguated by using a qualifier.*/
    inline fun <reified T : Any> get(
        qualifier: String, noinline parameters: ParametersDefinition? = null,
    ): T = getKoin().get(named(qualifier), parameters)

    /**
     * Retrieves an instance of type [T] from simulation environment, identified by [qualifier].
     *
     * @param T the type of instance to retrieve.
     * @param qualifier the name qualifier for the instance.
     * @param parameters an optional function that defines the parameters to be used for resolving instances.
     *
     * @return an instance of type [T] if found in simulation environment, otherwise null.
     */
    inline fun <reified T : Any> getOrNull(
        qualifier: String, noinline parameters: ParametersDefinition? = null,
    ): T? = getKoin().getOrNull(named(qualifier), parameters)

    val entityTrackingDefaults = SimEntityTrackingDefaults()

    init {

        // start console logger

//        addTraceListener { print(it) }
        if(enableComponentLogger) {
            enableComponentLogger()
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


        // self register the environment
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


    fun enableTickMetrics() {
        require(queue.none { it is TickMetrics }) {
            "The tick-metrics monitor is already registered"
        }

        TickMetrics(koin = getKoin())
    }

    val tickMetrics: MetricTimeline<Int>
        get() {
            val tm = queue.filterIsInstance<TickMetrics>()
                .firstOrNull()
            require(tm != null) { "Use enableTickMetrics=true to enable tick metrics" }
            return tm.timeline
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
//    @OptIn(AmbiguousDuration::class)
//    fun run(
//        duration: Duration? = null, until: Instant? = null, priority: Priority = NORMAL,
//    ) = run(duration, until, priority)

    /**
     * Start execution of the simulation. See https://www.kalasim.org/basics/#running-a-simulation
     *
     * @param duration Time to run
     * @param priority If a component has the same time on the event list, the main component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    @AmbiguousDuration
    fun run(
        duration: Number?,
        priority: Priority = NORMAL
    ) = run(duration?.toDuration(), null, priority)

    /**
     * Start execution of the simulation
     *
     * @param until Absolute time until the which the simulation should run. Requires tick-transform to be configured.
     * @param priority If a component has the same time on the event list, the main component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
//    @OptIn(AmbiguousDuration::class)
//    fun run(
//        until: Number, priority: Priority = NORMAL,
//    ) = run(until = until.toTickTime(), priority = priority)

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
//    @AmbiguousDuration
    fun run(
        duration: Duration? = null, until: SimTime? = null, priority: Priority = NORMAL, urgent: Boolean = false,
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

        pendingStandBy.removeFirstOrNull()
            ?.let {
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

    fun addEventListener(listener: EventListener) {
//        eventListeners.add(listener)
        eventListeners += listener
    }

    @Suppress("unused")
    fun removeEventListener(eventListener: EventListener) {
//        = eventListeners.remove(tr)
        eventListeners -= eventListener
    }


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

    internal fun push(component: Component, scheduledTime: SimTime, priority: Priority, urgent: Boolean) {
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
            require(scheduledComponents.map { it.name }
                .distinct().size == scheduledComponents.size) {
                "components must not have the same name"
            }
        }
    }

    override fun toJson() = json {
        "now" to now
        "queue" to queue.toList()
            .map { it.name }
            .toTypedArray()
    }

    fun hasAbsoluteTime() = startDate != null


    fun tick2wallTime(tickTime: TickTime): SimTime {
        return startDate!! + tickTransform.ticks2Duration(tickTime.value)
    }

    fun wall2TickTime(instant: SimTime): TickTime {
        val offsetDuration = instant - startDate!!

        return TickTime(tickTransform.durationAsTicks(offsetDuration))
    }

    fun Number.asSimTime() =  env.asSimTime(this)

    // deprecated because nothing should be logged outside a process
//    fun log(msg: String) = main.log(msg)
}


data class QueueElement(
    val component: Component, val time: SimTime, val priority: Priority, val queueCounter: Int, val urgent: Boolean
) : Comparable<QueueElement> {
    //TODO clarify if we need/want to also support urgent

    override fun compareTo(other: QueueElement): Int =
        compareValuesBy(this, other, { it.time }, { it.priority.value }, { it.queueCounter })

//    val heapSeq = if (urgent) -queueCounter else queueCounter

    override fun toString(): String {
//        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $queueCounter) : ${component.componentState}"
    }
}


fun Environment.enableComponentLogger() {
    require(eventListeners.none { it is ConsoleTraceLogger }) {
        "The component-logger is already registered"
    }

    addEventListener(ConsoleTraceLogger())
}


internal fun Environment.calcScheduleTime(until: SimTime?, duration: Duration?): SimTime {
    return if(until == null) {
        require(duration != null) { "neither duration nor till specified" }
        now + duration
    } else {
        require(duration == null) { "both duration and till specified" }
        until
    }
}


/** Register dependency in simulation context. For details see https://www.kalasim.org/basics/#dependency-injection */
inline fun <reified T> KoinModule.dependency(
    qualifier: Qualifier? = null, noinline definition: Definition<T>
) {
    single(qualifier = qualifier, createdAtStart = true, definition = definition)
}


/** Register dependency in simulation context. For details see https://www.kalasim.org/basics/#dependency-injection */
inline fun <reified T> Environment.dependency(qualifier: String, builder: Environment.() -> T) =
    dependency(named(qualifier), builder)

/** Register dependency in simulation context. For details see https://www.kalasim.org/basics/#dependency-injection */
inline fun <reified T> Environment.dependency(qualifier: Qualifier? = null, builder: Environment.() -> T): T {
    val something = builder(this)

    getKoin().loadModules(listOf(module(createdAtStart = true) {
        dependency(qualifier) { something }
    }))

    require(something !is Unit) {
        "dependency must not be last element in createSimulation{} as this is causing type inference to fail internally. Add println() or any other terminal statement to work around this problem."
    }

    return something
}


