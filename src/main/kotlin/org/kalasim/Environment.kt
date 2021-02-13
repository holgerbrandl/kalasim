package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.json.JSONObject
import org.kalasim.ComponentState.*
import org.kalasim.Defaults.DEFAULT_SEED
import org.kalasim.misc.JSON_INDENT
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.definition.Definition
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.time.Duration
import java.util.*


@Suppress("EXPERIMENTAL_FEATURE_WARNING")
internal inline class TickTime(val instant: Double)

const val MAIN = "main"

typealias KoinModule = org.koin.core.module.Module

//internal class EnvBuildContext : KoinModule() {
//    var enableConsoleLogger: Boolean = true
//}
// --> not possible because Module is not open

// https://github.com/InsertKoinIO/koin/issues/801
fun configureEnvironment(
    enableConsoleLogger: Boolean = false,
    builder: KoinModule.() -> Unit
): Environment =
    declareDependencies(builder).createSimulation(enableConsoleLogger) {}

fun declareDependencies(
    builder: KoinModule.() -> Unit
): KoinModule = module(createdAtStart = true) { builder() }


fun KoinModule.createSimulation(
    enableConsoleLogger: Boolean = false,
    useCustomKoin: Boolean = false,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit
): Environment = createSimulation(
    enableConsoleLogger = enableConsoleLogger,
    dependencies = this,
    useCustomKoin = useCustomKoin,
    randomSeed = randomSeed,
    builder = builder
)

fun createSimulation(
    enableConsoleLogger: Boolean = false,
    dependencies: KoinModule? = null,
    useCustomKoin: Boolean = false,
    randomSeed: Int = DEFAULT_SEED,
    builder: Environment.() -> Unit
): Environment =
    Environment(
        enableConsoleLogger = enableConsoleLogger,
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

@Suppress("EXPERIMENTAL_API_USAGE")
open class Environment(
    enableConsoleLogger: Boolean = false,
    dependencies: KoinModule? = null,
    koin: Koin? = null,
    randomSeed: Int = DEFAULT_SEED
) : KoinComponent {

    @Deprecated("serves no purposes and creates a memory leaks as objects are nowhere releases")
    private val components: MutableList<Component> = listOf<Component>().toMutableList()


    private var running: Boolean = false

    val rg: RandomGenerator = JDKRandomGenerator(randomSeed)
    val random: kotlin.random.Random = kotlin.random.Random(randomSeed.toLong())

    internal val nameCache = mapOf<String, Int>().toMutableMap()

    private val eventQueue = PriorityQueue<QueueElement>()

    /** Unmodifiable view on `eventQueue`. */
    val queue: List<Component>
        get() = eventQueue.map { it.component }


    private val eventListeners = listOf<EventListener>().toMutableList()


    val traceFilters = mutableListOf<EventFilter>()

    init {
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
    }

    var now = 0.0
        internal set

    fun now() = now

    /** Allows to transform ticks to real world time moements (represented by `java.time.Instant`) */
    var tickTransform: TickTransform? = null

    var curComponent: Component? = null
        private set

    var main: Component
        private set

    val _koin: Koin

    @Suppress("EXPERIMENTAL_OVERRIDE")
    override fun getKoin(): Koin = _koin

    init {

        // start console logger

//        addTraceListener { print(it) }
        if(enableConsoleLogger) {
            addEventListener(ConsoleTraceLogger(true))
        }

        _koin = koin ?: run {
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

        // declare dependencies
        if(dependencies != null) {
//            val deps = dependencies ?: (module(createdAtStart = true) { })
            getKoin().loadModules(listOf(dependencies), createEagerInstances = true)
//        KoinContextHandler.get()._scopeRegistry.rootScope.createEagerInstances()
//        startKoin { modules(koins) }
        }

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
    fun run(duration: Number? = null, until: Number? = null, priority: Priority = NORMAL, urgent: Boolean = false): Environment {
        // TODO https://simpy.readthedocs.io/en/latest/topical_guides/environments.html
        //  If you call it without any argument (env.run()), it steps through the simulation until there are
        //  no more events left.

        // TODO add test coverage for `until`

        if(duration == null) endOnEmptyEventlist = true

        val scheduledTime = calcScheduleTime(until, duration)

        main.reschedule(scheduledTime, priority, urgent, null, "run", SCHEDULED)

        running = true
        while(running) {
            step()
        }

        return (this)
    }

    /** Executes the next step of the future event list */
    private fun step() {

        pendingStandBy.removeIf { it.status != STANDBY }

        pendingStandBy.removeFirstOrNull()?.let {
            setCurrent(it, "standby")
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
            val t = if(endOnEmptyEventlist) {
                publishEvent(InteractionEvent(now, curComponent, null, null, "run end; no events left"))
                now
            } else {
                Double.MAX_VALUE
            }

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

    private fun setCurrent(c: Component, info: String? = null) {
        c.status = CURRENT
        c.scheduledTime = null

        curComponent = c
//        c.log(c, info)
    }


    internal fun addStandBy(component: Component) {
        standBy.add(component)
    }

    fun addEventListener(listener: EventListener) = eventListeners.add(listener)

    @Suppress("unused")
    fun removeEventListener(tr: EventListener) = eventListeners.remove(tr)


    internal fun publishEvent(event: Event) {
        if(traceFilters.any { it.matches(event) }) return

        eventListeners.forEach {
            if(it.filter == null || it.filter!!.matches(event)) it.consume(event)
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
        if(c.status == STANDBY) {
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

    fun push(component: Component, scheduledTime: Double, priority: Priority, urgent: Boolean) {
        queueCounter++

//        https://bezkoder.com/kotlin-priority-queue/
        // Remove an element from the Priority Queue => Dequeue the least element. The front of the Priority Queue
        // contains the least element according to the ordering, and the rear contains the greatest element.
        eventQueue.add(QueueElement(component, scheduledTime, Priority(-priority.value), queueCounter, urgent))

        // consistency checks
        require(queue.none(Component::isPassive)) { "passive component must not be in event queue" }
    }

    fun toJson(includeComponents: Boolean = false): JSONObject = json {
        if(includeComponents) {
            "components" to components.map { it.info.toJson() }
        }
        "num_components" to components.size
        "now" to now
        "queue" to queue.toList().map { it.name }.toTypedArray()
    }

    @Suppress("EXPERIMENTAL_OVERRIDE")
    override fun toString(): String {
        return toJson(false).toString(JSON_INDENT)
    }

    /** Transforms a wall `duration` into the corresponding amount of ticks.*/
    fun Duration.asTicks(): Double {
        require(tickTransform != null){ MISSING_TICK_TRAFO_ERROR }
        return tickTransform!!.durationAsTicks(this)
    }
}


data class QueueElement(
    val component: Component,
    val time: Double,
    val priority: Priority,
    val queueCounter: Int,
    val urgent: Boolean
) :
    Comparable<QueueElement> {
    //TODO clarify if we need/want to also support urgent

    override fun compareTo(other: QueueElement): Int =
        compareValuesBy(this, other, { it.time }, { it.priority.value }, { it.queueCounter })

//    val heapSeq = if (urgent) -queueCounter else queueCounter

    override fun toString(): String {
//        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $queueCounter) : ${component.status}"
    }
}


fun Environment.calcScheduleTime(till: Number?, duration: Number?): Double {
    return (till?.toDouble() to duration?.toDouble()).let { (till, duration) ->
        if(till == null) {
            if(duration == null) now else {
                now + duration
            }
        } else {
            require(duration == null) { "both duration and till specified" }
            till
        }
    }
}


inline fun <reified T> KoinModule.add(
    qualifier: Qualifier? = null,
    noinline definition: Definition<T>
) {
    single(qualifier = qualifier, createdAtStart = true, definition = definition)

}

public inline fun <reified T> Environment.dependency(builder: Environment.() -> T): T {
    val something = builder(this)
    getKoin().loadModules(listOf(
        module(createdAtStart = true) {
            add { something }
        }
    ), createEagerInstances = true)

    return something
}