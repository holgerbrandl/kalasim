package org.kalasim

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.isDistantFuture
import org.kalasim.Component.*
import org.kalasim.ComponentState.*
import org.kalasim.Priority.Companion.NORMAL
import org.kalasim.ResourceSelectionPolicy.*
import org.kalasim.ScheduledType.*
import org.kalasim.analysis.*
import org.kalasim.analysis.ResourceEventType.*
import org.kalasim.analysis.snapshot.ComponentSnapshot
import org.kalasim.misc.*
import org.kalasim.monitors.CategoryTimeline
import org.koin.core.Koin
import kotlin.math.*
import kotlin.reflect.*
import kotlin.time.Duration
import kotlin.time.DurationUnit


internal const val EPS = 1E-8

internal const val DEFAULT_QUEUE_PRIORITY = 0


/** Life-cycle states of a simulation components. See https://www.kalasim.org/component/#lifecycle */
enum class ComponentState {
    DATA, CURRENT, STANDBY, PASSIVE, INTERRUPTED, SCHEDULED, REQUESTING, WAITING
}


/** Indicate why a component is scheduled for later continuation/execution. */
enum class ScheduledType {
    HOLD, REQUEST, WAIT, ACTIVATE
}


/** Describes the priority of a process or request. Higher values will be scheduled earlier or claimed earlier respectively. */
data class Priority(val value: Int) : Comparable<Priority?> {
    companion object {
        val LOWEST = Priority(-20)
        val LOW = Priority(-10)
        val NORMAL = Priority(DEFAULT_QUEUE_PRIORITY)
        val IMPORTANT = Priority(20)
        val CRITICAL = Priority(30)
    }

    override fun compareTo(other: Priority?): Int = compareValuesBy(this, other ?: NORMAL) { value }
}

//class SimSequence(val component: Component) : Sequence<Component> {
//    override fun iterator(): Iterator<Component> {
//        return super.iterator()
//    }
//}
//
//public inline fun <T> Component.simSequence(crossinline iterator: () -> Iterator<T>): Sequence<T> = object : SimSequence<T> {
//    override fun iterator(): Iterator<T> = iterator()
//}


// TODO reassess if we should use these type-aliases
//typealias ProcessDefinition = SequenceScope<Component>
//private typealias ProcessContext = SequenceScope<Component>
//typealias ProcessDefinition = suspend SequenceScope<Component>.() -> Unit
//private typealias ProcessDefinition = Sequence<Component>


data class RequestContext(
    val requestId: Long,
    val quantity: Double,
    val priority: Priority?,
    val requestedAt: SimTime?,
    val honoredAt: SimTime?,
) {
    fun merge(rc: RequestContext): RequestContext {
        require(priority == rc.priority) { "Merging components with different priorities is not supported" }
        return copy(
            quantity = quantity + rc.quantity
        )
    }
}

/**
 * Represents a ticked component in the simulation. A ticked component provides in addition to `SimTime` interactions,
 * complementary methods that do work with TickTime instead
 *
 * @param name The name of the ticked component.
 * @param at The initial simulation time at which the ticked component is created.
 * @param delay The delay time before the ticked component becomes active.
 * @param priority The priority of the ticked component.
 * @param process The process pointer associated with the ticked component.
 * @param trackingConfig The component tracking configuration.
 * @param koin The Koin instance used for dependency injection.
 */
@AmbiguousDurationComponent
open class TickedComponent(
    name: String? = null,
    at: SimTime? = null,
    delay: Duration? = null,
    priority: Priority = NORMAL,
    process: GeneratorFunRef? = null,
    trackingConfig: ComponentTrackingConfig = ComponentTrackingConfig(),
    koin: Koin = DependencyContext.get(),
) : Component(name, at, delay, priority, process, koin, trackingConfig) {


    @AmbiguousDuration
    @Deprecated("use typed version hold(kotlin.time.Duration) instead")
    /**
     * Hold the component.
     *
     * For `hold` contract see [user manual](https://www.kalasim.org/component/#hold)
     *
     * @param duration Time to hold. Either `duration` or `till` must be specified.
     * @param until Absolute time until the component should be held
     * @param priority If a component has the same time on the event list, this component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    suspend fun SequenceScope<Component>.hold(
        duration: Number? = null,
        description: String? = null,
        until: SimTime? = null,
        priority: Priority = NORMAL,
        urgent: Boolean = false
    ) {
        yieldCurrent {
            this@TickedComponent.hold(duration?.toDuration(), description, until, priority, urgent)
        }
    }

    suspend fun SequenceScope<Component>.activate(
        at: SimTime? = null,
        delay: Number = 0,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        process: GeneratorFunRef? = null
    ) = yieldCurrent {
        this@TickedComponent.activate(
            at,
            delay.toDuration(),
            priority,
            urgent,
            keepRequest,
            keepWait,
            process as ProcessPointerWithArg<*>
        )
    }

    suspend fun <T : Any> SequenceScope<Component>.activate(
        process: ProcessPointerWithArgs<*, T>,
        processArgument: T,
        at: SimTime? = null,
        delay: Number = 0,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
    ) = yieldCurrent {
        this@TickedComponent.activate(
            process,
            processArgument,
            at,
            delay.toDuration(),
            priority,
            urgent,
            keepWait,
            keepRequest
        )
    }
}




/**
 * A kalasim component is used as component (primarily for queueing) or as a component with a process.
 * Usually, a component will be defined as a subclass of Component.
 *
 * For more information about components see [user manual](https://www.kalasim.org/component).
 *
 * @param name name of the component.  if the name ends with a period (.), auto-indexing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercase)
 * @param at Schedule time
 * @param delay Schedule with a delay if omitted, no delay
 * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
 * @param process The process to be started. If None (default), it will try to start self.process()
 * @param koin The dependency resolution context to be used to resolve the `org.kalasim.Environment`
 */
@OptIn(InternalKalasimApi::class)
open class Component(
    name: String? = null,
    at: SimTime? = null,
    delay: Duration? = null,
    priority: Priority = NORMAL,
    process: GeneratorFunRef? = null,
    koin: Koin = DependencyContext.get(),
    val trackingConfig: ComponentTrackingConfig = koin.getEnvDefaults().DefaultComponentConfig,

    // to be re-enabled/reworked as part of https://github.com/holgerbrandl/kalasim/issues/11
//    builder: SequenceScope<Component>.() -> Unit = {   }
) : SimulationEntity(name, koin) {


    private var oneOfRequest: Boolean = false

    internal val requests = mutableMapOf<Resource, RequestContext>()
    internal val claims = mutableMapOf<Resource, RequestContext>()

    private val waits = mutableListOf<StateRequest<*>>()

    /** Will be `true` if a component's request was not honored in time, or a wait predicate was not met before it timed outs. */
    var failed: Boolean = false
        private set

    private var waitAll: Boolean = false

    private var simProcess: SimProcess? = null


    // TODO 0.6 get rid of this field (not needed because can be always retrieved from eventList if needed
    //  What are performance implications?
    var scheduledTime: SimTime? = null
        internal set

    private var remainingDuration: Duration? = null

    fun interface ComponentStateChangeListener {
        fun stateChanged(component: Component)
    }

    internal val stateChangeListeners = mutableListOf<ComponentStateChangeListener>()

    /** Current lifecycle state of the component. See https://www.kalasim.org/component/#lifecycle for details. */
    var componentState: ComponentState = DATA
        internal set(value) {
            field = value

            stateTimeline.addValue(value)

            stateChangeListeners.forEach { it.stateChanged(this) }
        }


    val stateTimeline = CategoryTimeline(componentState, "status of ${this.name}", koin)

    init {

        with(trackingConfig) {
            stateTimeline.enabled = trackComponentState
        }

        log(trackingConfig.logCreation) {
            EntityCreatedEvent(now, env.currentComponent, this)
        }

        val overriddenProcess = javaClass.getMethod("process").declaringClass.simpleName != "Component"
        val overriddenRepeated = javaClass.getMethod("repeatedProcess").declaringClass.simpleName != "Component"
        val isCustomProcess = process != null && process.name != "process"
        val isNone = process != null && process.name == "none"

        if(!isCustomProcess) {
            require(!(overriddenProcess && overriddenRepeated)) {
                " So either override process or override repeatedProcess but not both"
            }
        }

        val generatorFunRef: GeneratorFunRef? = when {
            isNone -> null
            overriddenProcess -> ::process
            overriddenRepeated -> ::processLoop
            isCustomProcess -> process
            else -> null
        }

        this.simProcess = generatorFunRef?.ingestFunPointer()

        //  what's the point of scheduling it at `at`  without a process definition?
        //  --> main is one major reason, we need the engine to progress the time until a given point
        if(at != null || delay != null) {
            require(simProcess != null) {
                "component '${name}' must have process definition to be scheduled"
            }
        }

        val tickDelay = delay ?: Duration.ZERO

//        if (at != null || (process != null && (process.name != "process" || overriddenProcess))) {
        if(simProcess != null && this !is MainComponent) {
            val scheduledTime = if(at == null) {
                env.now + tickDelay
            } else {
                at + tickDelay
            }

            reschedule(scheduledTime, priority, false, null, ACTIVATE)
        }
    }

    private var lastProcess: ProcessReference? = null

    class ProcessReference(val generatorFunRef: GeneratorFunRef, vararg val arguments: Any)

    private fun GeneratorFunRef.ingestFunPointer(vararg processArgs: Any): SimProcess? {
        val isGenerator = returnType.toString().startsWith("kotlin.sequences.Sequence")

        require(isGenerator) {
            error("non-generating processes are no longer supported. If you feel this is a bug please file an issue at https://github.com/holgerbrandl/kalasim/issues")
        }

        lastProcess = ProcessReference(this, *processArgs)

        val sequence = if(parameters.run { isNotEmpty() && first().kind.name == "INSTANCE" }) {
            call(this@Component, *processArgs)
        } else {
            call(*processArgs)
        }

        return GenProcessInternal(this@Component, sequence, name)
    }


    /** Used to suppress component activation in presence of a process definition.*/
    fun none() = sequence<Component> {}

    open fun process() = sequence<Component> {
        TODO("Invalid state. Please file a bug report")
    }

    open fun repeatedProcess() = sequence<Component> {
        TODO("Invalid state. Please file a bug report")
    }

    internal fun processLoop() = sequence {
        while(true) {
            yieldAll(repeatedProcess())
        }
    }

    /** @return `true` if status is `PASSIVE`, `false` otherwise. */
    val isPassive: Boolean
        get() = componentState == PASSIVE

    /** @return `true` if status is `CURRENT`, `false` otherwise. */
    val isCurrent: Boolean
        get() = componentState == CURRENT

    /** @return `true` if status is `STANDBY`, `false` otherwise. */
    val isStandby: Boolean
        get() = componentState == STANDBY


    /** @return `true` if status is `SCHEDULED`, `false` otherwise. */
    val isScheduled: Boolean
        get() = componentState == SCHEDULED

    /** @return `true` if status is `INTERRUPTED`, `false` otherwise. */
    val isInterrupted: Boolean
        get() = componentState == INTERRUPTED

    /** @return `true` if status is `DATA`, `false` otherwise. */
    val isData: Boolean
        get() = componentState == DATA

    /** @return `true` if status is `REQUESTING`, `false` otherwise. */
    val isRequesting: Boolean
        get() = requests.isNotEmpty()


    /** @return `true` if waiting for a state to be honored, `false` otherwise. */
    val isWaiting: Boolean
        get() = waits.isNotEmpty()


    /**
     * Passivate a component.
     *
     * For `passivate()` contract see [user manual](https://www.kalasim.org/component/#passivate)
     */
    suspend fun SequenceScope<Component>.passivate(): Unit = yieldCurrent {
        this@Component.passivate()
    }

    /**
     * Passivate a component.
     *
     * For `passivate()` contract see [user manual](https://www.kalasim.org/component/#passivate)
     */
    fun passivate() {
        remainingDuration = if(componentState == CURRENT) {
            Duration.ZERO
        } else {
            requireNotData()
            requireNotInterrupted()
            remove()
            checkFail()
            scheduledTime!! - env.now
        }

        scheduledTime = null
        componentState = PASSIVE

        logStateChange()
    }


    fun logStateChange(
        details: String? = null,
        builder: () -> ComponentStateChangeEvent = {
            ComponentStateChangeEvent(
                now,
                env.currentComponent,
                this,
                componentState,
                details
            )
        }
    ) {
        log(trackingConfig.logStateChangeEvents) { builder() }
    }

    internal fun logInternal(enabled: Boolean, action: String) = log(enabled) {
        with(env) { InteractionEvent(now, currentComponent, this@Component, action) }
    }

    /**
     * Records a state-change event.
     *
     * @param action Describing the nature if the event
     */
    fun log(action: String) = env.apply { log(InteractionEvent(now, currentComponent, this@Component, action)) }


    private var interruptedStatus: ComponentState? = null

    /**
     * Interrupt level of an interrupted component  non interrupted components return 0.
     */
    private var interruptLevel = 0
        private set

    /** Interrupt the component.
     *
     * Can not be applied on the current component. Use `resume()` to resume. */
    fun interrupt() {
        require(componentState != CURRENT) { "Current component can no be interrupted" }

        if(componentState == INTERRUPTED) {
            interruptLevel++
        } else {
            requireNotData()
            remove()
            remainingDuration = scheduledTime?.minus(env.now)
            interruptLevel = 1
            interruptedStatus = componentState
            componentState = INTERRUPTED
        }

        logInternal(trackingConfig.logInteractionEvents, "interrupt (level=$interruptLevel)")
    }

    /**
     * Resumes an interrupted component.
     *
     * Can only be applied to interrupted components.
     * For the full contract definition, see [https*/
    fun resume(all: Boolean = false, priority: Priority = NORMAL) {
        // not part of original impl
        require(componentState == INTERRUPTED) { "Can only resume interrupted components" }
        require(interruptLevel > 0) { "interrupt level is expected to be greater than 0" }
        require(interruptedStatus != null) { "interrupt must be called before resume" }

        interruptLevel--

        if(interruptLevel != 0 && !all) {
            logInternal(trackingConfig.logInteractionEvents, "resume stalled (interrupt level=$interruptLevel)")
        } else {
            componentState = interruptedStatus!!

            logInternal(trackingConfig.logInteractionEvents, "resume ($componentState)")

            when(componentState) {
                PASSIVE -> {
                    logInternal(trackingConfig.logInteractionEvents, "passivate")
                }

                STANDBY -> {
                    scheduledTime = env.now
                    env.addStandBy(this)
                    logInternal(trackingConfig.logInteractionEvents, "standby")
                }

                in listOf(SCHEDULED, WAITING, REQUESTING) -> {
                    val reason = when(componentState) {
                        WAITING -> {
                            if(waits.isNotEmpty()) tryWait()
                            WAIT
                        }

                        REQUESTING -> {
                            tryRequest()
                            REQUEST
                        }

                        SCHEDULED -> {
                            HOLD
                        }

                        else -> TODO("map missing types")//"unknown"
                    }

                    reschedule(
                        env.now + remainingDuration!!,
                        priority,
                        urgent = false,
                        type = reason,
                    )

                }

                else -> error("Unexpected interrupt status $componentState is $name")
            }
        }

    }

    /**
     * Cancel a component (makes the component `DATA`).
     *
     * For `cancel` contract see [user manual](https://www.kalasim.org/component/#cancel)
     */
    suspend fun SequenceScope<Component>.cancel(): Unit = yieldCurrent {
        this@Component.cancel()
    }

    /**
     * Cancel a component (makes the component `DATA`).
     *
     * For `cancel` contract see [user manual](https://www.kalasim.org/component/#cancel)
     */
    fun cancel() {
        if(componentState != CURRENT) {
            requireNotData()
            requireNotInterrupted()
            remove()
            checkFail()
        }

        simProcess = null
        scheduledTime = null

        componentState = DATA

        logStateChange("canceled")
    }

    /**
     * Puts the component in standby mode.
     *
     * Not allowed for data components or main.
     *
     * For `standby` contract see [user manual](https://www.kalasim.org/component/#standby)
     */
    suspend fun SequenceScope<Component>.standby(): Unit = yieldCurrent {
        if(componentState != CURRENT) {
            requireNotMain()
            requireNotData()
            requireNotInterrupted()
            remove()
            checkFail()
        }

        scheduledTime = env.now
        env.addStandBy(this@Component)

        componentState = STANDBY

        logStateChange()
    }


    @Throws(InvalidRequestQuantity::class)
    private fun ensurePositiveQuantity(quantity: Number): Number {
        if(quantity.toDouble() < 0) {
            throw InvalidRequestQuantity("Positive quantity expected but was $quantity.")
        }
        return quantity
    }


    /**
     * Consumes capacity of a depletable resource. This is intentionally also suspendable, as a component may need to wait until a component has the capacity to consume the new quantity.
     *
     * For `take` contract see [user manual](https://www.kalasim.org/resource)
     *
     * @param quantity A quantity to be consumed from the depletable resource
     * @param priority When multiple components compete for taking from the same depletable resource, requests with higher priority will have precedence.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */    // note: in salabim this is called get, but we renamed it because it's an active process not passive reception
    suspend fun SequenceScope<Component>.take(
        resource: DepletableResource,
        quantity: Number = DEFAULT_REQUEST_QUANTITY,
        priority: Priority? = null,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
    ): Unit = request(
        resource withQuantity ensurePositiveQuantity(quantity) andPriority priority,
        failAt = failAt,
        failDelay = failDelay,
        failPriority = failPriority
    )


    /** Define error behavior if `put` exceeds the capacity of a depletable resource .*/
    enum class CapacityLimitMode {

        /** Fail if request size exceeds resource capacity.*/
        FAIL,

        /** Schedule put if necessary, hoping for a later capacity increase.*/
        SCHEDULE,

        /** Cap put requests at capacity level */
        CAP
    }


    /**
     * Restores capacity of a depletable resource. This is intentionally also suspendable, as a component may need to wait until a component has the capacity to consume the new quantity.
     *
     * For `put` contract see [user manual](https://www.kalasim.org/resource/#depletable-resources)
     *
     * @param resource A depletable resource.
     * @param quantity The quantity to be refilled (default = 1).
     * @param priority When multiple components compete for putting on the same resource, requests with higher priority will have precedence.
     * @param description An optional description of the put request's nature/reason/cause.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */
    suspend fun SequenceScope<Component>.put(
        resource: DepletableResource,
        quantity: Number = DEFAULT_REQUEST_QUANTITY,
        priority: Priority? = null,
        description: String? = null,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        capacityLimitMode: CapacityLimitMode = CapacityLimitMode.FAIL,
    ) = put(
        ResourceRequest(resource, quantity.toDouble(), priority),
        description = description,
        failAt = failAt,
        failDelay = failDelay,
        failPriority = failPriority,
        capacityLimitMode = capacityLimitMode
    )

    /**
     * Restores capacity of anonymous resources. This is intentionally also suspendable, as a component may need to wait until a component has the capacity to consume the new quantity.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resourceRequests Each `ResourceRequest` is a tuple of resource, quantity (default=-1 for put requests) and priority (default NORMAL). It can be created with the following DSl `resource withQuantity 3 and Priority HIGH`
     * @param description An optional description of the put request's nature/reason/cause.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */
    suspend fun SequenceScope<Component>.put(
        vararg resourceRequests: ResourceRequest,
        description: String? = null,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        capacityLimitMode: CapacityLimitMode = CapacityLimitMode.FAIL,
    ) = request(
        *resourceRequests
            .map { it.copy(quantity = -1.0 * ensurePositiveQuantity(it.quantity).toDouble()) }
            .toTypedArray(),
        description = description,
        failAt = failAt,
        failDelay = failDelay,
        failPriority = failPriority,
        capacityLimitMode = capacityLimitMode
    )

    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resources Resources to be requested with a quantity of 1 and priority null.
     * @param quantity The quantity to be requested from each resource.
     * @param priority If multiple components compete for the same resource, requests with higher priority will have precedence.
     * @param description An optional description of the request nature/reason/cause.
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given. It is possible to check which resource has been claimed with `Component.claimers()`.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly.
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */
    suspend fun SequenceScope<Component>.request(
        resources: Collection<Resource>,
        description: String? = null,
        quantity: Number = DEFAULT_REQUEST_QUANTITY,
        priority: Priority? = null,
        oneOf: Boolean = false,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        capacityLimitMode: CapacityLimitMode = CapacityLimitMode.FAIL,
        honorBlock: (suspend SequenceScope<Component>.(RequestScopeContext) -> Unit)? = null
    ) = request(
        *resources.map { it withQuantity quantity andPriority priority }.toTypedArray(),
        description = description,
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf,
        failPriority = failPriority,
        capacityLimitMode = capacityLimitMode,
        honorBlock = honorBlock
    )


    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resources Resources to be requested with default quantity and default priority.
     * @param quantity The quantity to be requested from each resource.
     * @param priority If multiple components compete for the same resource, requests with higher priority will have precedence.
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given. It is possible to check which resource has been claimed with `Component.claimers()`.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */
    suspend fun SequenceScope<Component>.request(
        vararg resources: Resource,
        description: String? = null,
        quantity: Number = DEFAULT_REQUEST_QUANTITY,
        priority: Priority? = null,
        oneOf: Boolean = false,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        capacityLimitMode: CapacityLimitMode = CapacityLimitMode.FAIL,
        honorBlock: (suspend SequenceScope<Component>.(RequestScopeContext) -> Unit)? = null
    ) = request(
        *resources.map { it withQuantity quantity andPriority priority }.toTypedArray(),
        description = description,
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf,
        failPriority = failPriority,
        capacityLimitMode = capacityLimitMode,
        honorBlock = honorBlock
    )


    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resourceRequests Each `ResourceRequest` is a tuple of resource, quantity (default=1) and priority (default 0).  It can be created with the following DSl `resource withQuantity 3 and Priority HIGH`
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given. It is possible to check which resource has been claimed with `Component.claimers()`.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly
     *
     * @sample org.kalasim.dokka.resourceHowTo
     */
    suspend fun SequenceScope<Component>.request(
        vararg resourceRequests: ResourceRequest,
        description: String? = null,
        oneOf: Boolean = false,
        urgent: Boolean = false,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        capacityLimitMode: CapacityLimitMode = CapacityLimitMode.FAIL,
        // try to avoid argument by inferring from stacktrace
//        calledFrom: String? = null,
        // see https://stackoverflow.com/questions/46098105/is-there-a-way-to-open-and-close-a-stream-easily-at-kotlin
        honorBlock: (suspend SequenceScope<Component>.(RequestScopeContext) -> Unit)? = null
    ) {
        val requestedAt = now

        @Suppress("NAME_SHADOWING")
        val resourceRequests: List<ResourceRequest> = when(capacityLimitMode) {
            CapacityLimitMode.FAIL -> resourceRequests.firstOrNull { abs(it.quantity) > it.resource.capacity }?.let {
                throw CapacityLimitException(
                    it.resource,
                    "Request of quantity ${it.quantity} can never succeed",
                    now,
                    it.resource.capacity
                )
            } ?: resourceRequests.asList()

            CapacityLimitMode.SCHEDULE -> resourceRequests.asList()

            CapacityLimitMode.CAP -> resourceRequests.map {
                with(it) {
                    if(quantity < 0 && resource is DepletableResource) {
                        copy(quantity = max(resource.level - resource.capacity, quantity))
//                        copy(quantity = -1 * max(resource.capacity - resource.level, quantity))
                    } else {
                        throw RuntimeException("CAP mode is just supported for put requests")
                    }
                }
            }
        }

        yieldCurrent {
            if(componentState != CURRENT) {
                requireNotMain()
                requireNotData()
                requireNotInterrupted()
                remove()
                checkFail()
            }

            require(requests.isEmpty()) { "no pending requests are allowed when requesting" }
            // fails because of org.kalasim.test.ResourceTests#it should report correct resource in honor block when using oneOf mode
//            require(claims.isEmpty()) { "no open claims are allowed when requesting" }

            require(failAt == null || failDelay == null) { "Either failAt or failDelay can be specified, not both together" }

            scheduledTime = when {
                failAt != null -> failAt
                failDelay != null -> env.now + failDelay
                else -> SimTime.DISTANT_FUTURE
            }

            failed = false
            oneOfRequest = oneOf

            resourceRequests.forEach { (resource, quantity, priority) ->

                if(resource.preemptive && resourceRequests.size > 1) {
                    throw IllegalArgumentException("preemptive resources do not support multiple resource requests")
                }

                //            // TODO clarify intent here
                //            if (calledFrom == "put") {
                //                q = -q
                //            }

                require(quantity >= 0 || resource.depletable) { "quantity <0" }

                val requestContext = RequestContext(random.nextLong().absoluteValue, quantity, priority, now, null)

                //  is same resource is specified several times, just add them up
                //https://stackoverflow.com/questions/53826903/increase-value-in-mutable-map
                // todo this may not not be correct for a RelaxedFCFS honor policy or a SQF --> replace entirely with list?
                requests.merge(resource, requestContext, RequestContext::merge)

                resource.requesters.add(this@Component, priority = priority)

                if(resource.trackingConfig.logResourceChanges) {
                    log(
                        ResourceEvent(
                            env.now,
                            requestContext.requestId,
                            env.currentComponent,
                            this@Component,
                            resource,
                            REQUESTED,
                            quantity,
                            priority
                        )
                    )
                }

                if(resource.preemptive) {
                    var av = resource.available
                    val thisClaimers = resource.claimers.q

                    val bumpCandidates = mutableListOf<Component>()
                    //                val claimComponents = thisClaimers.map { it.c }
                    for(cqe in thisClaimers.toList().reversed()) {
                        if(av >= quantity) {
                            break
                        }

                        // check if prior of component
                        if((priority?.value ?: 0) <= (cqe.priority?.value ?: 0)) {
                            break
                        }

                        av += cqe.component.claims[resource]!!.quantity
                        bumpCandidates.add(cqe.component)
                    }

                    if(av >= 0) {
                        bumpCandidates.forEach {
                            it.releaseInternal(resource, bumpedBy = this@Component)
                            logInternal(
                                trackingConfig.logInteractionEvents,
                                "$it bumped from $resource by ${this@Component}"
                            )

                            it.activate()
                        }
                    }
                }
            }

            requests.forEach { (resource, requestContext) ->
                val (_, quantity, _, _) = requestContext
                if(quantity < resource.minq)
                    resource.minq = quantity
            }

            tryRequest()

            if(requests.isNotEmpty()) {
                reschedule(scheduledTime!!, priority = failPriority, urgent = urgent, description, REQUEST)
            }
        }

        if(honorBlock != null) {
            // suspend{ ... }
            val honoredAt = now

            honorBlock(RequestScopeContext(if(oneOf) claims.toList().last().first else null, requestedAt))

            val releasedAt = now

            // salabim says: It is possible to check which resource has been claimed with `Component.claimers()`.
            // note we could alternative also use the request-id to identify the claim
            resourceRequests.filter { it.resource.claimers.contains(this@Component) }.forEach {
                release(it)

                if(it.resource.trackingConfig.trackActivities) {
                    val rse =
                        ResourceActivityEvent(
                            requestedAt,
                            honoredAt,
                            releasedAt,
                            this@Component,
                            it.resource,
                            description,
                            it.quantity
                        )
                    (it.resource.activities as MutableList<ResourceActivityEvent>).add(rse)
                    log(rse)
                }
            }
        }
    }


    /** Determine if all current requests of this component could be honored. */
    private fun honorAll(): List<Pair<Resource, RequestContext>>? {
        for((resource, requestContext) in requests) {
            val requestedQuantity = requestContext.quantity

            if(requestedQuantity < 0) {
                require(resource is DepletableResource) {
                    "can not request negative quantity from non-depletable resource"
                }
            }

            if(!resource.canComponentHonorQuantity(this, requestedQuantity)) return null
        }

        return requests.toList()
    }


    private fun honorAny(): List<Pair<Resource, RequestContext>>? {
        for((resource, requestContext) in requests) {
            val requestedQuantity = requestContext.quantity

            if(resource.canComponentHonorQuantity(this, requestedQuantity)) {
                return listOf(resource to requestContext)
            }
//            if (requestedQuantity > 0) {
//                if (requestedQuantity <= resource.capacity - resource.claimed + EPS) {
//                    return listOf(resource to requestedQuantity)
//                }
//            } else if (-requestedQuantity <= resource.claimed + EPS) {
//                return listOf(resource to requestedQuantity)
//            }
        }

        return null
    }


    /**
     * Check if any or all (depending on on-of-setting)  request(s) to resources can be honored, and perform the
     * claims accordingly.
     *
     * @return `true` if the pending request(s) were honored
     */
    internal fun tryRequest(): Boolean {
        if(componentState == INTERRUPTED) return false

        val rHonor: List<Pair<Resource, RequestContext>>? = if(oneOfRequest) honorAny() else honorAll()

        if(rHonor.isNullOrEmpty()) return false

        requests.forEach { (resource, requestContext) ->

            // proceed just if request was honored claim it
            if(rHonor.any { it.first == resource }) {
                val quantity = requestContext.quantity
                resource.claimed += quantity //this will also update the timeline

                log(trackingConfig.logInteractionEvents) {
                    val type = when {
                        resource !is DepletableResource -> CLAIMED
                        quantity < 0 -> PUT
                        else -> TAKE
                    }
                    ResourceEvent(
                        env.now,
                        requestContext.requestId,
                        env.currentComponent,
                        this,
                        resource,
                        type,
                        quantity,
                        requestContext.priority
                    )
                }

                if(!resource.depletable) {
                    val thisPrio = resource.requesters.q.firstOrNull { it.component == this }?.priority
                    claims.merge(resource, requestContext.copy(honoredAt = now), RequestContext::merge)

                    //also register as claimer in resource if not yet present
                    if(resource.claimers.q.none { it.component == this }) {
                        resource.claimers.add(this, thisPrio)
                    }
                }
            }

            resource.removeRequester(this)
        }

        requests.clear()
        remove()

        val honorInfo = rHonor.firstOrNull()!!.first.name + (if(rHonor.size > 1) "++" else "")

        reschedule(now, NORMAL, false, "Request honored by $honorInfo", ACTIVATE)

        // process negative put requests
        rHonor.filter { it.first.depletable }.forEach {
//            require(it.first.depletable)
            it.first.tryRequest()
        }


        return true
    }


    /**
     * Check whether component is bumped from a resource
     *
     * @param resource - resource to be checked; if omitted, checks whether component belongs to any resource claimers
     *
     * @return `true` if this component is not in the resource claimers
     */
    fun isBumped(resource: Resource? = null): Boolean = !isClaiming(resource)

    /**
     * Check whether component is claiming from a resource
     *
     * @param resource resource to be checked; if omitted, checks whether component belongs to any resource claimers
     *
     * @return `true` if this component is in the resource claimers
     */
    @Suppress("SpellCheckingInspection")
    fun isClaiming(resource: Resource? = null): Boolean {
        @Suppress("IfThenToElvis")
        return if(resource == null) {
            TODO("claiming test without resource is not yet implemented as this would require a registry in SimulationEntity")
//            env.queue.filter{ it is ComponentQueue<*> }.map{(it as ComponentQueue<*>).contains(this)}
//            for q in self._qmembers:
//            if hasattr(q, "_isclaimers"): True
//            return False
        } else {
            resource.claimers.contains(this)
        }
    }


    // kept in Component API for api compatibility with salabim
    private fun remove() {
        env.remove(this)
    }

    /**
     * Stops the current simulation while preserving its queue and process state, and returns to the call site of `run()`.
     * See https://www.kalasim.org/basics/#running-a-simulation).  */
    suspend fun SequenceScope<Component>.stopSimulation() {
        yield(env.main.activate())
    }


    fun terminate() {
        // todo strictly speaking this should fail in assert mode = FULL, as the user should take care to release claims
        // we need to wrap claims as another map to avoid concurrent modification
        claims.toMutableMap().forEach { (resource, _) ->
            release(resource)
        }

        componentState = DATA
        scheduledTime = null
        simProcess = null

        logStateChange("Ended")
    }

    private fun requireNotData() =
        require(componentState != DATA) { "data component '$name' not allowed" }

    private fun requireNotInterrupted() =
        require(!isInterrupted) { "interrupted component '$name' needs to be resumed prior to interaction" }

    private fun requireNotMain() =
        require(this != env.main) { "main component not allowed" }

    @InternalKalasimApi
    // todo rename to `schedule` because re is not really part of contract
    fun reschedule(
        scheduledTime: SimTime,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        description: String? = null,
        type: ScheduledType
    ) {
        require(scheduledTime >= env.now) {
            "scheduled time (${scheduledTime}) before now (${env.now})"
        }

        if(ASSERT_MODE == AssertMode.FULL) {
            require(this !in env.queue) {
                "component must not be in queue when rescheduling but must be removed already at this point"
            }
        }

        val newStatus: ComponentState = when(type) {
            WAIT -> WAITING
            REQUEST -> REQUESTING
            HOLD, ACTIVATE -> SCHEDULED
        }

        componentState = newStatus

        this.scheduledTime = scheduledTime

        require(this.scheduledTime != null) { "reschedule with null time is unlikely to have meaningful semantics" }

        if(this.scheduledTime != null) {
            env.push(this, scheduledTime, priority, urgent)
        }

        log(trackingConfig.logStateChangeEvents) {
            RescheduledEvent(
                now,
                env.currentComponent,
                this,
                description,
                scheduledTime,
                type
            )
        }
    }


    /**
     * Activate a component.
     *
     * This method is used to activate a component. The activation process starts a specified process with the given arguments.
     *
     * Please refer to the [user manual](https://www.kalasim.org/component/#activate) for more information on the `activate` contract.
     *
     * @param process The name of the process to be started. If set to `None`, the process will not be changed. If the
     * component is a data component, the generator function `process` will be used as the default process. Optionally type safe
     * arguments can be provided to the generator function  via `processArgument` and `otherProcessArgument`
     * @param at The schedule time. If omitted, no `delay` is used.
     * @param delay The delay before starting the process. It uses a `Duration` object to specify the delay amount. The default value is `Duration.ZERO`.
     * @param priority The priority level of the activation. It uses the `Priority` enumeration with options HIGH, NORMAL, and LOW. The default value is NORMAL.
     * @param urgent Indicates whether the activation is urgent or not. If set to true, the activation will be treated as urgent. The default value is false.
     * @param keepRequest Indicates whether to keep the activation request even after the process is started. If set to true, the activation request will be kept. The default value is false.
     * @param keepWait Indicates whether to keep waiting for the process to complete before returning. If set to true, the activation will not return until the process is complete. The default value is false.
     *
     * @return The activated component.
     */
    suspend fun SequenceScope<Component>.activate(
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        process: GeneratorFunRef? = null
    ) = yieldCurrent {
        this@Component.activateInternal(
            process?.let { ProcessReference(it) },
            at,
            delay,
            priority,
            urgent,
            keepRequest,
            keepWait,
        )
    }


    /**
     * Activate a component.
     *
     * This method is used to activate a component. The activation process starts a specified process with the given arguments.
     *
     * Please refer to the [user manual](https://www.kalasim.org/component/#activate) for more information on the `activate` contract.
     *
     * @param process The name of the process to be started. If set to `None`, the process will not be changed. If the
     * component is a data component, the generator function `process` will be used as the default process. Optionally type safe
     * arguments can be provided to the generator function  via `processArgument` and `otherProcessArgument`
     * @param processArgument The argument to be passed to the process.
     * @param at The schedule time. If omitted, no `delay` is used.
     * @param delay The delay before starting the process. It uses a `Duration` object to specify the delay amount. The default value is `Duration.ZERO`.
     * @param priority The priority level of the activation. It uses the `Priority` enumeration with options HIGH, NORMAL, and LOW. The default value is NORMAL.
     * @param urgent Indicates whether the activation is urgent or not. If set to true, the activation will be treated as urgent. The default value is false.
     * @param keepRequest Indicates whether to keep the activation request even after the process is started. If set to true, the activation request will be kept. The default value is false.
     * @param keepWait Indicates whether to keep waiting for the process to complete before returning. If set to true, the activation will not return until the process is complete. The default value is false.
     *
     * @return The activated component.
     */
    suspend fun <T : Any> SequenceScope<Component>.activate(
        process: ProcessPointerWithArg<T>,
        processArgument: T,
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,

        ) = yieldCurrent {
        this@Component.activateInternal(
            ProcessReference(process as GeneratorFunRef, processArgument),
            at,
            delay,
            priority,
            urgent,
            keepRequest,
            keepWait
        )
    }

    /**
     * Activate a component.
     *
     * This method is used to activate a component. The activation process starts a specified process with the given arguments.
     *
     * Please refer to the [user manual](https://www.kalasim.org/component/#activate) for more information on the `activate` contract.
     *
     * @param process The name of the process to be started. If set to `None`, the process will not be changed. If the
     * component is a data component, the generator function `process` will be used as the default process. Optionally type safe
     * arguments can be provided to the generator function  via `processArgument` and `otherProcessArgument`
     * @param processArgument The argument to be passed to the process.
     * @param at The schedule time. If omitted, no `delay` is used.
     * @param delay The delay before starting the process. It uses a `Duration` object to specify the delay amount. The default value is `Duration.ZERO`.
     * @param priority The priority level of the activation. It uses the `Priority` enumeration with options HIGH, NORMAL, and LOW. The default value is NORMAL.
     * @param urgent Indicates whether the activation is urgent or not. If set to true, the activation will be treated as urgent. The default value is false.
     * @param keepRequest Indicates whether to keep the activation request even after the process is started. If set to true, the activation request will be kept. The default value is false.
     * @param keepWait Indicates whether to keep waiting for the process to complete before returning. If set to true, the activation will not return until the process is complete. The default value is false.
     *
     * @return The activated component.
     */
    suspend fun <T : Any, S : Any> SequenceScope<Component>.activate(
        process: ProcessPointerWithArgs<T, S>,
        processArgument: T,
        otherProcessArgument: S,
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,

        ) = yieldCurrent {
        this@Component.activateInternal(
            ProcessReference(process as GeneratorFunRef, processArgument, otherProcessArgument),
            at,
            delay,
            priority,
            urgent,
            keepRequest,
            keepWait
        )
    }

    /**
     * Activate a component.
     *
     * This method is used to activate a component. The activation process starts a specified process with the given arguments.
     *
     * Please refer to the [user manual](https://www.kalasim.org/component/#activate) for more information on the `activate` contract.
     *
     * @param process The name of the process to be started. If set to `None`, the process will not be changed. If the
     * component is a data component, the generator function `process` will be used as the default process. Optionally type safe
     * arguments can be provided to the generator function  via `processArgument` and `otherProcessArgument`
     * @param at The schedule time. If omitted, no `delay` is used.
     * @param delay The delay before starting the process. It uses a `Duration` object to specify the delay amount. The default value is `Duration.ZERO`.
     * @param priority The priority level of the activation. It uses the `Priority` enumeration with options HIGH, NORMAL, and LOW. The default value is NORMAL.
     * @param urgent Indicates whether the activation is urgent or not. If set to true, the activation will be treated as urgent. The default value is false.
     * @param keepRequest Indicates whether to keep the activation request even after the process is started. If set to true, the activation request will be kept. The default value is false.
     * @param keepWait Indicates whether to keep waiting for the process to complete before returning. If set to true, the activation will not return until the process is complete. The default value is false.
     *
     * @return The activated component.
     */
    fun activate(
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        process: ProcessPointerWithArg<*>? = null
    ): Component {
        activateInternal(process?.let { ProcessReference(it) }, at, delay, priority, urgent, keepRequest, keepWait)
        return this
    }

    /**
     * Activate a component.
     *
     * This method is used to activate a component. The activation process starts a specified process with the given arguments.
     *
     * Please refer to the [user manual](https://www.kalasim.org/component/#activate) for more information on the `activate` contract.
     *
     * @param process The name of the process to be started. If set to `None`, the process will not be changed. If the
     * component is a data component, the generator function `process` will be used as the default process. Optionally type safe
     * arguments can be provided to the generator function  via `processArgument` and `otherProcessArgument`
     * @param processArgument The argument to be passed to the process.
     * @param at The schedule time. If omitted, no `delay` is used.
     * @param delay The delay before starting the process. It uses a `Duration` object to specify the delay amount. The default value is `Duration.ZERO`.
     * @param priority The priority level of the activation. It uses the `Priority` enumeration with options HIGH, NORMAL, and LOW. The default value is NORMAL.
     * @param urgent Indicates whether the activation is urgent or not. If set to true, the activation will be treated as urgent. The default value is false.
     * @param keepRequest Indicates whether to keep the activation request even after the process is started. If set to true, the activation request will be kept. The default value is false.
     * @param keepWait Indicates whether to keep waiting for the process to complete before returning. If set to true, the activation will not return until the process is complete. The default value is false.
     *
     * @return The activated component.
     */
    fun <T : Any> activate(
        process: ProcessPointerWithArgs<*, T>,
        processArgument: T,
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
//        process: ProcessPointer? = Component::process
    ): Component {

        activateInternal(
            ProcessReference(process as GeneratorFunRef, processArgument),
            at,
            delay,
            priority,
            urgent,
            keepRequest,
            keepWait
        )

        return this
    }


    /**
     * Activate component
     *
     * For `activate` contract see [user manual](https://www.kalasim.org/component/#activate)
     *
     * @param at Schedule time
     * @param delay Schedule with a delay if omitted, no delay is used
     * @param process Name of process to be started.
     * * if None (default), process will not be changed
     * * if the component is a data component, the generator function `process` will be used as the default process.
     */
    fun <T : Any, S : Any> activate(
        process: ProcessPointerWith2Args<*, T, S>,
        processArgument: T,
        otherArgument: S,
        at: SimTime? = null,
        delay: Duration = Duration.ZERO,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
//        process: ProcessPointer? = Component::process
    ): Component {

        activateInternal(
            ProcessReference(process as GeneratorFunRef, processArgument, otherArgument),
            at,
            delay,
            priority,
            urgent,
            keepRequest,
            keepWait
        )

        return this
    }

    private fun activateInternal(
        process: ProcessReference?,
        at: SimTime?,
        delay: Duration,
        priority: Priority,
        urgent: Boolean,
        keepRequest: Boolean,
        keepWait: Boolean,

        ) {
        require(componentState != CURRENT || process != null) {
            // original contract
            "Can not activate the CURRENT component. If needed simply use hold method."
            // technically we could use suspend here , but since activate is used
            // outside of process definitions we don't want to over-complete the API for this
            // rare edge case
            // workaround yield(activate(process = Component::process))
        }

        // todo why this convoluted logic??
        val processReference: ProcessReference? = process
            ?: if(componentState == DATA) {
                lastProcess
            } else {
                null
            }

        var extra = ""

        if(processReference != null) {
            this.simProcess = processReference.generatorFunRef.ingestFunPointer(*processReference.arguments)

            extra = "process=${processReference.generatorFunRef.name}"
        }

        if(componentState != CURRENT) {
            remove()
            if(processReference != null) {
                if(!(keepRequest || keepWait)) {
                    checkFail()
                }
            } else {
                checkFail()
            }
        }

        val scheduledTime = if(at == null) {
            env.now + delay
        } else {
            at + delay
        }

        reschedule(scheduledTime, priority, urgent, "Activating $extra", ACTIVATE)
    }

    internal fun checkFail() {
        if(requests.isNotEmpty()) {
            logInternal(trackingConfig.logInteractionEvents, "request failed")
            requests.forEach { it.key.removeRequester(this) }
            requests.clear()
            failed = true
        }

        if(waits.isNotEmpty()) {
            logInternal(trackingConfig.logInteractionEvents, "wait failed")
            waits.forEach { it.state.waiters.remove(this) }

            waits.clear()
            failed = true
        }
    }


    /**
     * Holds the component. For details see [user manual](https://www.kalasim.org/component/#hold)
     *
     * This method is used to hold the component for a specific duration, with an optional description,
     * until a specific simulation time, with an optional priority, and an optional urgent flag.
     *
     * The component will be held for the specified duration or until the next simulation event,
     * whichever comes first. If a duration is not provided, the component will be held until the
     * next simulation event. If a description is provided, it can be used to identify the hold
     * operation when analyzing the simulation results. If an until parameter is provided, the
     * component will be held until the specified simulation time. If a priority is provided, it will
     * determine the order in which the component will be scheduled if there are other components
     * with the same hold duration. A higher priority value indicates a higher priority. If the
     * urgent flag is set to true, the component will be scheduled with the highest possible priority.
     *
     * @param duration The duration for which the component should be held. If not provided, the
     * component will be held until the next simulation event.
     * @param description An optional description for the hold operation.
     * @param until The simulation time until which the component should be held. If provided, the
     * component will be held until the specified simulation time.
     * @param priority The priority of the hold operation. A higher priority value indicates a higher
     * priority. Defaults to NORMAL.
     * @param urgent A flag indicating whether the hold operation is urgent. If set to true, the
     * component will be scheduled with the highest possible priority. Defaults to false.
     */
    suspend fun SequenceScope<Component>.hold(
        duration: Duration? = null,
        description: String? = null,
        until: SimTime? = null,
        priority: Priority = NORMAL,
        urgent: Boolean = false
    ) = yieldCurrent {
        this@Component.hold(duration, description, until, priority, urgent)
    }


    /**
     * Hold the component.
     *
     * For `hold` contract see [user manual](https://www.kalasim.org/component/#hold)
     *
     * @param duration Time to hold. Either `duration` or `till` must be specified.
     * @param until Absolute time until the component should be held
     * @param priority If a component has the same time on the event list, this component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    fun hold(
        duration: Duration? = null,
        description: String? = null,
        until: SimTime? = null,
        priority: Priority = NORMAL,
        urgent: Boolean = false
    ) {
        if(componentState != PASSIVE && componentState != CURRENT) {
            requireNotData()
            requireNotInterrupted()
            remove()
            checkFail()
        }

        val scheduledTime = env.calcScheduleTime(until, duration)

        reschedule(scheduledTime, priority, urgent, description, HOLD)
    }


    fun getThis() = this

    internal fun callProcess() {
        require(simProcess != null) { "component '${name}' must have active process to be called" }
        simProcess!!.call()
    }

    /**
     * Release a quantity from a resource or resources.
     *
     * It is not possible to release from an anonymous resource, this way.
     * Use Resource.release() in that case.
     *
     * @param resource The resource to be released
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(resource: Resource, quantity: Number = Double.MAX_VALUE) =
        release(ResourceRequest(resource, quantity.toDouble()))


    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.
     */
    fun release(vararg resources: Resource) =
        release(*resources.map { it withQuantity Double.MAX_VALUE }.toTypedArray())


    /**
     * Releases the specified resources.
     *
     * It is not possible to release from an anonymous resource, this way.
     * Use Resource.release() in that case.
     *
     * @param  releaseRequests  A list of resource requests. Each request is formed by a
     *  * resource
     *  * an optional priority
     *  * an optional quantity to be requested from the resource
     *     </ul>
     */
    fun release(vararg releaseRequests: ResourceRequest) {
        for((resource, quantity) in releaseRequests) {
            require(!resource.depletable) { " It is not possible to release from an depletable resource, this way. Use Resource.release() in that case." }

            releaseInternal(resource, quantity)
        }

        if(releaseRequests.isEmpty()) {
            logInternal(trackingConfig.logInteractionEvents, "Releasing all claimed resources $claims")

            for((r, _) in claims) {
                releaseInternal(r)
            }
        }
    }

    // todo move this function into Resource
    private fun releaseInternal(resource: Resource, q: Double? = null, bumpedBy: Component? = null) {
        require(resource in claims) { "$name not claiming from resource ${resource.name}" }

        val requestContext = claims[resource]!!

        val releaseQuantity = if(q == null) {
            requestContext.quantity
        } else if(q > requestContext.quantity) {
            requestContext.quantity
        } else {
            q
        }

        resource.claimed -= releaseQuantity

        log(resource.trackingConfig.logResourceChanges) {
            ResourceEvent(
                env.now,
                requestContext.requestId,
                env.currentComponent,
                this,
                resource,
                RELEASED,
                releaseQuantity,
                requestContext.priority,
                bumpedBy = bumpedBy
            )
        }

        claims[resource] = with(requestContext) { copy(quantity = this.quantity - releaseQuantity) }

        if(claims[resource]!!.quantity < EPS) {
            leave(resource.claimers)
            claims.remove(resource)
        }

        // check for rounding errors salabim.py:12290
        require(!resource.claimers.isEmpty() || resource.claimed == 0.0) { "rounding error in claimed quantity" }
        // fix if(claimers.isEmpty()) field= 0.0

        if(bumpedBy == null) resource.tryRequest()
    }


    /**
     * Wait for any or all of the given [state](https://www.kalasim.org/state) values are met.
     *
     * For `wait` contract see [user manual](https://www.kalasim.org/component/#wait)
     *
     * @sample org.kalasim.dokka.statesHowTo
     *
     * @param state A state variable
     * @param waitFor The state value to wait for
     * @param description The description of the wait request.
     * @param triggerPriority The queue priority to be used along with a [state change trigger](https://www.kalasim.org/state/#state-change-triggers)
     * @param failAt If the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay If the request is not honored before `now + failDelay`,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     */
    suspend fun <T> SequenceScope<Component>.wait(
        state: State<T>,
        waitFor: T,
        description: String? = null,
        triggerPriority: Priority = NORMAL,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL
    ) = wait(
        StateRequest(state, priority = triggerPriority) { state.value == waitFor },
        description = description,
        failPriority = failPriority,
        failAt = failAt,
        failDelay = failDelay,
    )

    /**
     * Wait for the given [state](https://www.kalasim.org/state) predicate to be met.
     *
     * For `wait` contract see [user manual](https://www.kalasim.org/component/#wait)
     *
     * @sample org.kalasim.dokka.statesHowTo
     *
     * @param state The state variable to wait for.
     * @param triggerPriority The queue priority to be used along with a state [state change trigger](https://www.kalasim.org/state/#state-change-triggers)
     * @param description The description of the wait request.
     * @param failAt If the request is not honored before failAt, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay If the request is not honored before now + failDelay, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param predicate The predicate on the state to wait for.
     */
    suspend fun <T> SequenceScope<Component>.wait(
        state: State<T>,
        triggerPriority: Priority = NORMAL,
        description: String? = null,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        predicate: (T) -> Boolean
    ) = wait(
        StateRequest(state, predicate = predicate, priority = triggerPriority),
        description = description,
        failPriority = failPriority,
        failAt = failAt,
        failDelay = failDelay,
    )


    /**
     * Wait for any or all of the given state values are met
     *
     * For `wait` contract see [user manual](https://www.kalasim.org/component/#wait)
     *
     * @sample org.kalasim.dokka.statesHowTo
     *
     * @param stateRequests Requests indicating a state and a target condition or predicate for fulfilment
     * @param description The description of the wait request.
     * @param failAt If the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay Skip and set `failed` if the request is not honored before `now + failDelay`,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param failPriority Schedule priority of the fail event. If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param all If `false`, continue, if any of the given state/values is met. if `true` (default), continue if all of the given state/values are met.
     */
    suspend fun SequenceScope<Component>.wait(
        vararg stateRequests: StateRequest<*>,
        description: String? = null,
        urgent: Boolean = false,
        failAt: SimTime? = null,
        failDelay: Duration? = null,
        failPriority: Priority = NORMAL,
        all: Boolean = true
    ) = yieldCurrent {
        if(componentState != CURRENT) {
            requireNotMain()
            requireNotData()
            requireNotInterrupted()
            remove()
            checkFail()
        }

        waitAll = all

        require(failAt == null || failDelay == null) { "Either failAt or failDelay can be specified, not both together" }

        scheduledTime = when {
            failAt != null -> failAt
            failDelay != null -> env.now + failDelay
            else -> SimTime.DISTANT_FUTURE
        }

        stateRequests
            // skip already tracked states
            .filterNot { sr -> waits.any { it.state == sr.state } }
            .forEach { sr ->
                val (state, srPriority, _) = sr
                state.waiters.add(this@Component, srPriority)
                waits.add(sr)
            }

        tryWait()

        if(waits.isNotEmpty()) {
            reschedule(
                scheduledTime!!,
                priority = failPriority,
                urgent = urgent,
                description = description,
                type = WAIT
            )
        }
    }

    internal fun tryWait(): Boolean {
        if(componentState == INTERRUPTED) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        val honored = if(waitAll) {
            waits.all { sr ->
                (sr as StateRequest<Any>).let {
                    it.predicate(it.state.value)
                }
            }
        } else {
            waits.any { sr ->
                (sr as StateRequest<Any>).let {
                    it.predicate(it.state.value)
                }
            }
        }


        if(honored) {
            waits.forEach { sr ->
                sr.state.waiters.remove(this)
            }

            waits.clear()
            remove()

            reschedule(env.now, NORMAL, false, null, WAIT)
        }

        return honored
    }

    /**
     * Leave queue
     *
     * @param q Queue queue to leave
     */
    fun leave(q: ComponentQueue<*>) {
//        log("Leaving ${q.name}")
        @Suppress("UNCHECKED_CAST")
        (q as ComponentQueue<Component>).remove(this)
    }


    override val snapshot: ComponentSnapshot
        get() = ComponentSnapshot(this)


    internal suspend fun SequenceScope<Component>.yieldCurrent(builder: () -> Unit = {}) {
        val initialStatus = componentState

        require(initialStatus == CURRENT) {
            """Component context violated. Kalasim has detected an invalid call of 
               an outer scope function. This can happen if a sub-process defined 
               within a component class is calling methods of the parent scope. 
               Unfortunately, this can't be resolved at compile time.
           """.trimIndent()
        }

        builder()

        if(initialStatus == CURRENT) {
            yield(this@Component)
        }
    }


    suspend fun SequenceScope<Component>.selectResource(
        resources: List<Resource>,
        quantity: Number = 1,
        policy: ResourceSelectionPolicy = RandomOrder
    ): Resource {
        require(resources.isNotEmpty()) { "Resources listing must not be empty" }

        val selected = when(policy) {
            ShortestQueue -> {
                resources.minByOrNull { it.requesters.size }!!
            }

            RoundRobin -> {
                // note could also be achieved with listOf<Resource>().repeat().iterator()
                val mapKey = listOf(this.hashCode(), resources.map { it.name }.hashCode()).hashCode()
                // initialize if not yet done
                val curValue = SELECT_SCOPE_IDX.putIfAbsent(mapKey, 0) ?: 0

                // increment for future calls
                SELECT_SCOPE_IDX[mapKey] = (curValue + 1).rem(resources.size)

                return resources[curValue]
            }

            FirstAvailable -> {
                while(resources.all { it.available < quantity.toDouble() }) {
                    standby()
                }

                resources.first { it.available > quantity.toDouble() }
            }

            RandomOrder -> {
                resources[discreteUniform(0, resources.size - 1).sample()]
            }

            RandomAvailable -> {
                val available = resources.filter { it.available >= quantity.toDouble() }
                require(available.isNotEmpty()) { "Not all resources must be in use to use RandomAvailable selection policy" }

                available[discreteUniform(0, available.size - 1).sample()]
            }
        }

        return selected
    }


    /** Consume a queue into groups of elements (a.k.a batches).
     *
     * For details see [user manual](https://www.kalasim.org/component/#queue)
     *
     * @param queue The queue to be consumed
     * @param batchSize The size of the batch to be created. A positive integer is expected here.
     * @param timeout An optional timeout describing how long it shall wait before forming an incomplete/empty batch
     * @return A list of type <T> of size `batchSize` or lesser (and potentially even empty) if timed out before filling the batch.
     */
    suspend fun <T : SimulationEntity> SequenceScope<Component>.batch(
        queue: ComponentQueue<T>,
        batchSize: Int,
        timeout: Duration? = null
    ): List<T> {
        // Note: Adopted from simmer::batch (Ucar2019, p14)

        require(batchSize > 0) { "Batch size must be positive" }

        val queueListener = object : CollectionChangeListener<T>() {
            override fun added(component: T) {
                if(queue.size >= batchSize) {
                    this@Component.activate()
                }
            }
        }

        if(queue.size < batchSize) {
            queue.addChangeListener(queueListener)
            if(timeout != null) hold(timeout) else passivate()
        }

        val actualBatchSize = min(batchSize, queue.size)
        val batch = List(actualBatchSize) { queue.poll() }

        queue.removeChangeListener(queueListener)

        return batch
    }


    /**
     * Suspends the execution of the current sequence scope until all the components in the provided [components] list
     * have their state changed to [DATA].
     *
     * @param components The list of components to wait for.
     */
    suspend fun SequenceScope<Component>.join(vararg components: Component) = join(components.toList())

    /**
     * Suspends the execution of the current sequence scope until all the components in the provided [components] list
     * have their state changed to [DATA].
     *
     * @param components The list of components to wait for.
     */
    suspend fun SequenceScope<Component>.join(components: List<Component>) =
        wait(*components.map { it.componentState() turns ComponentState.DATA }.toTypedArray(), all=true)
//        wait(*components.map { it.componentState() turns DATA }.toTypedArray<StateRequest<ComponentState>>())


    companion object{
        val logger  = KotlinLogging.logger{}
//        val logger : KLogger
//    get() = KotlinLogging.logger(javaClass.canonicalName)

    }
}

internal fun Koin.getEnvDefaults(): SimEntityTrackingDefaults = get<Environment>().entityTrackingDefaults

internal val SELECT_SCOPE_IDX = mutableMapOf<Int, Int>()


/** Create a state component to lifecycle monitoring using a https://www.kalasim.org/state/. */
class LifecycleState(val component: Component) : State<ComponentState>(component.componentState) {
    init {
//        changeListeners.add { value = component.componentState }
        component.stateChangeListeners.add{
            value = component.componentState
        }
    }
}

/** Create a state component to lifecycle monitoring using a https://www.kalasim.org/state/. */
fun Component.componentState() = LifecycleState(this)
//fun Component.componentState() = State(componentState).apply {
//    stateChangeListeners.add{ value = componentState }
//}

enum class ResourceSelectionPolicy {
    ShortestQueue, FirstAvailable, RandomOrder, RandomAvailable, RoundRobin
}


//
// Abstract component process to be either generator or simple function
//

typealias GeneratorFunRef = KFunction<Sequence<Component>>
typealias ProcessPointerWithArg<T> = KFunction1<T, Sequence<Component>>
typealias ProcessPointerWithArgs<T, S> = KFunction2<T, S, Sequence<Component>>
typealias ProcessPointerWith2Args<C, T, S> = KFunction3<C, T, S, Sequence<Component>>


interface SimProcess {
    fun call()

    val name: String
}

class GenProcessInternal(val component: Component, seq: Sequence<Component>, override val name: String) :
    SimProcess {

    val iterator = seq.iterator()

    override fun call() {
        try {
            iterator.next()
        } catch(e: NoSuchElementException) {
            if(e.message != null) e.printStackTrace()
            component.terminate()
        }
    }
}

// Disabled because never used and seems obsolete. Just kept for salabim-compat until a first major release
//class SimpleProcessInternal(val component: Component, val funPointer: ProcessPointer, override val name: String) :
//    SimProcess {
//    override fun call() {
//        funPointer.call(component)
//    }
//}

internal const val DEFAULT_REQUEST_QUANTITY = 1.0

data class ResourceRequest(
    val resource: Resource,
    val quantity: Double = DEFAULT_REQUEST_QUANTITY,
    val priority: Priority? = null
)

infix fun Resource.withQuantity(quantity: Number) = ResourceRequest(this, quantity.toDouble())
infix fun Resource.withPriority(priority: Int) = ResourceRequest(this, priority = Priority(priority))
infix fun Resource.withPriority(priority: Priority) = ResourceRequest(this, priority = priority)

infix fun ResourceRequest.andPriority(priority: Priority?) = ResourceRequest(this.resource, this.quantity, priority)
infix fun ResourceRequest.andPriority(priority: Int) = andPriority(Priority(priority))

data class RequestScopeContext(val resource: Resource?, val requestingSince: SimTime)

//    data class StateRequest<T>(val s: State<T>, val value: T? = null, val priority: Int? = null)
data class StateRequest<T>(val state: State<T>, val priority: Priority? = null, val predicate: (T) -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as StateRequest<*>

        if(state != other.state) return false
        if(predicate != other.predicate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + predicate.hashCode()
        return result
    }
}

infix fun <T> State<T>.turns(value: T) = StateRequest(this) { it == value }

internal fun formatWithInf(env: Environment?, time: SimTime?): String =
    if(time == null || time.isDistantFuture) "<inf>" else formatWithInf(env?.asTickTime(time))

internal fun formatWithInf(time: TickTime?): String =
    if(time == null || time.value == Double.MAX_VALUE || time.value.isInfinite()) "<inf>" else TRACE_DF.format(time.value)


data class ComponentLifecycleRecord(
    val component: String,
    val createdAt: SimTime,
    val inDataSince: SimTime?,
    val inData: Duration,
    val inCurrent: Duration,
    val inStandby: Duration,
    val inPassive: Duration,
    val inInterrupted: Duration,
    val inScheduled: Duration,
    val inRequesting: Duration,
    val inWaiting: Duration
)

@OptIn(AmbiguousDuration::class)
fun Component.toLifeCycleRecord(): ComponentLifecycleRecord {
    val c = this

    val histogram = c.stateTimeline.summed().mapValues{ it.value.toDouble(DurationUnit.MINUTES)}

    return ComponentLifecycleRecord(
        c.name,
        c.creationTime,
        inDataSince = if(c.isData) c.stateTimeline.statsData().timepoints.last() else null,
        env.asDuration(histogram[DATA] ?: 0.0),
        env.asDuration(histogram[CURRENT] ?: 0.0),
        env.asDuration(histogram[STANDBY] ?: 0.0),
        env.asDuration(histogram[PASSIVE] ?: 0.0),
        env.asDuration(histogram[INTERRUPTED] ?: 0.0),
        env.asDuration(histogram[SCHEDULED] ?: 0.0),
        env.asDuration(histogram[REQUESTING] ?: 0.0),
        env.asDuration(histogram[WAITING] ?: 0.0),
    )
}


class InvalidRequestQuantity(msg: String) : Throwable(msg)


// just needed to expose certain internal functions to experts
//object InternalCompomnentUtil{
//    internal fun reschedule(
//        component: Component,
//        scheduledTime: SimTime,
//        priority: Priority = Priority.NORMAL,
//        urgent: Boolean = false,
//        description: String? = null,
//        type: ScheduledType
//    ) {
//        component.reschedule()
//    }
