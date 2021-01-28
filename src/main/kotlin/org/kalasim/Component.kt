package org.kalasim

import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.ComponentState.*
import org.kalasim.ResourceEventType.*
import org.kalasim.ResourceSelectionPolicy.*
import org.kalasim.misc.Jsonable
import org.kalasim.misc.TRACE_DF
import org.kalasim.monitors.FrequencyLevelMonitor
import org.koin.core.Koin
import org.koin.core.component.KoinApiExtension
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import java.util.*
import kotlin.math.min
import kotlin.reflect.KFunction1


internal const val EPS = 1E-8

@Deprecated("directly use sequence<Component>{}")
typealias ProcContext = SequenceScope<Component>

enum class ComponentState {
    DATA, CURRENT, STANDBY, PASSIVE, INTERRUPTED, SCHEDULED, REQUESTING, WAITING
}

internal const val DEFAULT_QUEUE_PRIORITY = 0

inline class Priority(val value: Int)

val LOWER = Priority(-10)
val NORMAL = Priority(DEFAULT_QUEUE_PRIORITY)
val HIGH = Priority(10)


/**
 * A kalasim component is used as component (primarily for queueing) or as a component with a process.
 * Usually, a component will be defined as a subclass of Component.
 *
 *
 *  @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)
 * @param at schedule time
 * @param delay schedule with a delay if omitted, no delay
 * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
 * @param process  of process to be started.  if None (default), it will try to start self.process()
 * @param koin The dependency resolution context to be used to resolve the `org.kalasim.Environment`
 */
open class Component(
    name: String? = null,
    at: Number? = null,
    delay: Number = 0,
    priority: Priority = NORMAL,
    process: ProcessPointer? = Component::process,
    koin: Koin = GlobalContext.get()
) :
//    KoinComponent,
    SimulationEntity(name, koin) {

    private var oneOfRequest: Boolean = false

    internal val requests = mapOf<Resource, Double>().toMutableMap()
    private val waits = listOf<StateRequest<*>>().toMutableList()
    val claims = mapOf<Resource, Double>().toMutableMap()

    var failed: Boolean = false
        private set

    private var waitAll: Boolean = false

    private var simProcess: SimProcess? = null


    // TODO 0.6 get rid of this field (not needed because can be always retrieved from eventList if needed
    //  What are performance implications?
    var scheduledTime: Double? = null

    private var remainingDuration : Double? = null

    var status: ComponentState = DATA
        set(value) {
            field = value
            statusMonitor.addValue(value)
        }

    val statusMonitor = FrequencyLevelMonitor(status, "status of ${name}", koin)


    init {
        val dataSuffix = if(process == null && this.name != MAIN) " data" else ""
        env.addComponent(this)
        log(now, env.curComponent, this, "create", dataSuffix)


        // if its a generator treat it as such
        this.simProcess = ingestFunPointer(process)

        if(process != null) {
            val scheduledTime = if(at == null) {
                env.now + delay.toDouble()
            } else {
                at.toDouble() + delay.toDouble()
            }

            reschedule(scheduledTime, priority, false, null, "activate", SCHEDULED)
        }

        @Suppress("LeakingThis")
        setup()
    }


    private fun ingestFunPointer(process: ProcessPointer?): SimProcess? {
//        if(process != null ){
//            print("param type is " + process!!.returnType)
//            if(process!!.returnType.toString().startsWith("kotlin.sequences.Sequence"))
//        }

        return if(process != null) {
            val isGenerator = process.returnType.toString().startsWith("kotlin.sequences.Sequence")

            if(isGenerator) {
                @Suppress("UNCHECKED_CAST")
                val sequence = process.call(this)
                GenProcessInternal(this, sequence, process.name)
            } else {
                TODO("add test coverage here")
                SimpleProcessInternal(this, process, process.name)
            }
        } else {
            null
        }
    }

    /**  called immediately after initialization of a component.
     * by default this is a dummy method, but it can be overridden.
     * */
    @Deprecated("use inheritance instead and do additional setup bits in child class constructor")
    open fun setup() {
    }

    /** The current simulation time*/
    val now
        get() = env.now


    open fun process() = this.let {
        sequence<Component> {
//            while (true) { // disabled because too much abstraction
//            process(it)
//            process()
//            }
        }
    }

    /** Generator function that implements "process". This can be overwritten in component classes a convenience alternative to process itself.*/
    // no longer needed  and redundant API
//    open suspend fun ProcContext.process(it: Component) {}

    // no longer needed  and redundant API
//    open suspend fun ProcContext.process() {}

    /** @return `true` if status is `PASSIVE`, `false` otherwise. */
    val isPassive: Boolean
        get() = status == PASSIVE

    /** @return `true` if status is `CURRENT`, `false` otherwise. */
    val isCurrent: Boolean
        get() = status == CURRENT

    /** @return `true` if status is `STANDBY`, `false` otherwise. */
    val isStandby: Boolean
        get() = status == STANDBY


    /** @return `true` if status is `SCHEDULED`, `false` otherwise. */
    val isScheduled: Boolean
        get() = status == SCHEDULED

    /** @return `true` if status is `INTERRUPTED`, `false` otherwise. */
    val isInterrupted: Boolean
        get() = status == INTERRUPTED

    /** @return `true` if status is `DATA`, `false` otherwise. */
    val isData: Boolean
        get() = status == DATA

    /** @return `true` if status is `REQUESTING`, `false` otherwise. */
    val isRequesting: Boolean
        get() = requests.isNotEmpty()


    /** @return `true` if waiting for a state to be honored, `false` otherwise. */
    val isWaiting: Boolean
        get() = waits.isNotEmpty()

    // TODO v0.4 reenable


    /** Passivate a component.
     *
     * For `passivate` contract see [user manual](https://www.kalasim.org/component/#passivate)
     */
    suspend fun SequenceScope<Component>.passivate(): Unit = yieldCurrent {
        this@Component.passivate()
    }

    /** Passivate a component.
     *
     * For `passivate` contract see [user manual](https://www.kalasim.org/component/#passivate)
     */
    fun passivate() {
        remainingDuration = if(status == CURRENT) {
            0.0
        } else {
            requireNotData()
            remove()
            checkFail()
            scheduledTime!! - env.now
        }

        scheduledTime = null
        status = PASSIVE

        log(now, env.curComponent, this, "passivate")
    }


    private var interruptedStatus: ComponentState? = null

    /** interrupt level of an interrupted component  non interrupted components return 0. */
    var interruptLevel = 0
        private set

    /** Interrupt the component.
     *
     * Can not be applied on the curent component. Use `resume()` to resume. */
    fun interrupt() {
        require(status != CURRENT) { "Current component can no be interrupted" }

        if(status == INTERRUPTED) {
            interruptLevel++
        } else {
            requireNotData()
            remove()
            remainingDuration = scheduledTime?.minus(env.now)
            interruptLevel = 1
            interruptedStatus = status
            status = INTERRUPTED
        }

        log("interrupt (level=$interruptLevel)")
    }

    /** Resumes an interrupted component. Can only be applied to interrupted components.
     *
     * For the full contract definition see https://www.kalasim.org/component/#interrupt

     * @param all If `true`, the component returns to the original status, regardless of the number of interrupt levels if
     * `false` (default), the interrupt level will be decremented and if the level reaches 0, the component will return
     * to the original status.
     * @param priority If a component has the same time on the event list, this component is sorted according to
    the priority.
     */
    fun resume(all: Boolean = false, priority: Priority = NORMAL) {
        // not part of original impl
        require(status == INTERRUPTED) { "Can only resume interrupted components" }
        require(interruptLevel > 0) { "interrupt level is expected to be greater than 0" }
        require(interruptedStatus != null) { "interrupt must be called before resume" }

        interruptLevel--

        if(interruptLevel != 0 && !all) {
            log("resume stalled (interrupt level=$interruptLevel)")
        } else {
            status = interruptedStatus!!

            log("resume ($status)")

            when(status) {
                PASSIVE -> {
                    log("passivate")
                }
                STANDBY -> {
                    scheduledTime = env.now
                    env.addStandBy(this)
                    log("standby")
                }
                in listOf(SCHEDULED, WAITING, REQUESTING) -> {
                    val reason = when(status) {
                        WAITING -> {
                            if(waits.isNotEmpty()) tryWait()
                            "wait"
                        }
                        REQUESTING -> {
                            tryRequest()
                            "request"
                        }
                        SCHEDULED -> {
                            "hold"
                        }
                        else -> "unknown"
                    }

                    reschedule(
                        env.now + remainingDuration!!,
                        priority,
                        urgent = false,
                        caller = reason,
                        newStatus = status
                    )

                }
                else -> error("Unexpected interrupt status ${status} is $name")
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
        if(status != CURRENT) {
            requireNotData()
            remove()
            checkFail()
        }

        simProcess = null
        scheduledTime = null

        status = DATA

        log(now, env.curComponent, this, "cancel")
    }

    /**
     * Puts the component in standby mode.
     *
     * Not allowed for data components or main.
     *
     * For `standby` contract see [user manual](https://www.kalasim.org/component/#standby)
     */
    suspend fun SequenceScope<Component>.standby(): Unit = yieldCurrent {
        if(status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        scheduledTime = env.now
        env.addStandBy(this@Component)

        status = STANDBY

        log(now, env.curComponent, this@Component)
    }


    /** Equivalent to request.*/
    //TODO seems redundant but is referred to in docs
    // commented because conflicts with KoinComponent.get()
//    suspend fun SequenceScope<Component>.get(): Nothing = ImplementMe()


    /**
     * Request anonymous resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  if the request is not honored before now+fail_delay,
     *
     * @sample org.kalasim.scratch.ResourceDocu.main
     */
    suspend fun SequenceScope<Component>.put(
        vararg resourceRequests: ResourceRequest,
        failAt: Number? = null,
        failDelay: Number? = null,
    ) = request(
        *resourceRequests.map { it.copy(quantity = -it.quantity) }.toTypedArray(),
        failAt = failAt, failDelay = failDelay
    )

    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resources Resources to be requested with a quantity of 1 and priority null.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  if the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly
     * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     *
     * @sample org.kalasim.scratch.ResourceDocu.main
     */
    suspend fun SequenceScope<Component>.request(
        resources: Collection<Resource>,
        failAt: Number? = null,
        failDelay: Number? = null,
        oneOf: Boolean = false,
        priority: Priority = NORMAL,
        honorBlock: (suspend SequenceScope<Component>.() -> Any)? = null
    ) = request(
        *resources.map { it withQuantity DEFAULT_REQUEST_QUANTITY }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf,
        priority = priority,
        honorBlock = honorBlock
    )


    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resources Resources to be requested with default quantity and default priority.
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  if the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly
     * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     *
     * @sample org.kalasim.scratch.ResourceDocu.main
     */
    suspend fun SequenceScope<Component>.request(
        vararg resources: Resource,
        // todo review if this should rather be a number (and dist at call site)
        failAt: Number? = null,
        failDelay: Number? = null,
        oneOf: Boolean = false,
        priority: Priority = NORMAL,
        honorBlock: (suspend SequenceScope<Component>.() -> Any)? = null
    ) = request(
        *resources.map { it withQuantity DEFAULT_REQUEST_QUANTITY }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf,
        priority = priority,
        honorBlock = honorBlock
    )


    /**
     * Request from a resource or resources.
     *
     * For `request` contract see [user manual](https://www.kalasim.org/component/#request)
     *
     * @param resourceRequests Each `ResourceRequest` is a tuple of resource, quantity (default=1) and priority (default 0).
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  if the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param oneOf If `true`, just one of the requests has to be met (or condition), where honoring follows the order given.
     * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     * @param honorBlock If provided, it will wait until resource requests are honored, execute the block, and release the resources accordingly
     *
     * @sample org.kalasim.scratch.ResourceDocu.main
     */
    suspend fun SequenceScope<Component>.request(
        vararg resourceRequests: ResourceRequest,
        //todo change to support distribution parameters instead
        failAt: Number? = null,
        failDelay: Number? = null,
        oneOf: Boolean = false,
        //todo use type here and not string
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        // try to avoid argument by inferring from stacktrace
        calledFrom: String? = null,
        // see https://stackoverflow.com/questions/46098105/is-there-a-way-to-open-and-close-a-stream-easily-at-kotlin
        honorBlock: (suspend SequenceScope<Component>.() -> Any)? = null
    ) {
        yieldCurrent {
            if(status != CURRENT) {
                requireNotData()
                requireNotMain()
                remove()
                checkFail()
            }

            require(requests.isEmpty()) { "no pending requests are allowed when requesting" }
            require(requests.isEmpty()) { "no open claims are allowed when requesting" }

            require(failAt == null || failDelay == null) { "Either failAt or failDelay can be specified, not both together" }

            scheduledTime = when {
                failAt != null -> failAt.toDouble()
                failDelay != null -> env.now + failDelay.toDouble()
                else -> Double.MAX_VALUE
            }

            failed = false
            oneOfRequest = oneOf

            resourceRequests.forEach { (r, quantity, priority) ->
                var q = quantity

                if(r.preemptive && resourceRequests.size > 1) {
                    throw IllegalArgumentException("preemptive resources do not support multiple resource requests")
                }

                //            // TODO clarify intent here
                //            if (calledFrom == "put") {
                //                q = -q
                //            }

                require(q >= 0 || r.anonymous) { "quantity <0" }

                //  is same resource is specified several times, just add them up
                //https://stackoverflow.com/questions/53826903/increase-value-in-mutable-map
                requests.merge(r, q, Double::plus)

                val reqText =
                    (calledFrom ?: "") + "Requesting ${q} from ${r.name} with priority ${priority} and oneof=${oneOf}"

                //            enterSorted(r.requesters, priority)
                r.requesters.add(this@Component, priority = priority)

                log(
                    now,
                    env.curComponent,
                    this@Component,
                    reqText
                )

                if(r.preemptive) {
                    var av = r.availableQuantity
                    val thisClaimers = r.claimers.q

                    val bumpCandidates = mutableListOf<Component>()
                    //                val claimComponents = thisClaimers.map { it.c }
                    for(cqe in thisClaimers) {
                        if(av >= q) {
                            break
                        }

                        // check if prior of component
                        if((priority?.value ?: 0) <= (cqe.priority?.value ?: 0)) {
                            break
                        }

                        av += cqe.component.claims.getOrDefault(r, 0.0)
                        bumpCandidates.add(cqe.component)
                    }

                    if(av >= 0) {
                        bumpCandidates.forEach {
                            it.releaseInternal(r, bumpedBy = this@Component)
                            log("$it bumped from $r by ${this@Component}")
                            it.activate()
                        }
                    }
                }
            }

            requests.forEach { (resource, quantity) ->
                if(quantity < resource.minq)
                    resource.minq = quantity
            }

            tryRequest()

            if(requests.isNotEmpty()) {
                reschedule(
                    scheduledTime!!, priority = priority, urgent = urgent,
                    caller = "request", newStatus = REQUESTING
                )
            }
        }

        if(honorBlock != null) {
            // suspend{ ... }
            honorBlock()

            release(*resourceRequests)
        }
    }


    // TODO what is the reasoning here
    private fun honorAll(): List<Pair<Resource, Double>>? {
        for((r, requestedQuantity) in requests) {
            if(requestedQuantity > r.capacity - r.claimed + EPS) {
                return null
            } else if(-requestedQuantity > r.claimed + EPS) {
                return null
            }
        }

        return requests.toList()
    }

    private fun honorAny(): List<Pair<Resource, Double>>? {
        for(request in requests) {
            val (r, requestedQuantity) = request
            if(requestedQuantity > r.capacity - r.claimed + EPS) {
                return listOf(r to requestedQuantity)
            } else if(-requestedQuantity <= r.claimed + EPS) {
                return listOf(r to requestedQuantity)
            }
        }

        return null
    }

    internal open fun tryRequest(): Boolean {
        if(status == INTERRUPTED) return false

        val rHonor = if(oneOfRequest) honorAny() else honorAll()

        if(rHonor.isNullOrEmpty()) return false

        requests
//            .filterNot { it.key.anonymous }
            .forEach { (resource, quantity) ->
                // proceed just if request was honored claim it
                if(rHonor.any { it.first == resource }) {
                    resource.claimed += quantity //this will also update the monitor

                    log(
                        ResourceEvent(
                            env.now,
                            this, resource,
                            env.curComponent,
                            CLAIMED,
                            quantity,
                            resource.capacity,
                            resource.claimed
                        )
                    )

                    if(!resource.anonymous) {
                        val thisPrio = resource.requesters.q.firstOrNull { it.component == this }?.priority
                        claims.merge(resource, quantity, Double::plus)

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

        reschedule(now, NORMAL, false, null, "Request honored by $honorInfo", SCHEDULED)

        // process negative put requests (todo can't we handle them separately)
        rHonor.filter { it.first.anonymous }.forEach { it.first.tryRequest() }

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


//    @Deprecated("no longer needed, handled by queue directly")
//    private fun enterSorted(requesters: Queue<Component>, priority: Int) {
//    }


    // kept in Component API for api compatibility with salabim
    private fun remove() {
        env.remove(this)
    }

    fun terminate() {
        // we need to wrap claims as another map to avoid concurrent modification
        claims.toMutableMap().forEach { (resource, _) ->
            release(resource)
        }

        status = DATA
        scheduledTime = null
        simProcess = null

        log(now, env.curComponent, this, "Ended")
    }

    private fun requireNotData() =
        require(status != DATA) { "data component '$name' not allowed" }

    private fun requireNotMain() =
        require(this != env.main) { "main component not allowed" }

    internal fun reschedule(
        scheduledTime: Double,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        description: String? = null,
        caller: String? = null,
        newStatus: ComponentState,
    ) {
        require(scheduledTime >= env.now) { "scheduled time (${scheduledTime}) before now (${env.now})" }
        require(this !in env.queue) { "component must not be in queue when reschudling but must be removed already at this point" }

        status = newStatus

        this.scheduledTime = scheduledTime

        require(this.scheduledTime != null) { "reschedule with null time is unlikely to have meaningful semantics" }

        if(this.scheduledTime != null) {
            env.push(this, scheduledTime, priority, urgent)
        }

        //todo implement extra
        val extra = "scheduled for ${formatWithInf(scheduledTime)}"
        // line_no: reference to source position
        // 9+ --> continnue generator
        // 13 --> no plus means: generator start


        // calculate scheduling delta
        val delta = if(this.scheduledTime == env.now || (this.scheduledTime == Double.MAX_VALUE)) "" else {
            "+" + TRACE_DF.format(scheduledTime - env.now) + " "
        }

        // print trace
        log(now, env.curComponent, this, ("$caller $delta ${description ?: ""}").trim(), extra)
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
    fun activate(
        at: Number? = null,
        delay: Number = 0,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        process: ProcessPointer? = null
//        process: ProcessPointer? = Component::process

    ): Component {

        require(status != CURRENT || process != null) {
            // original contract
            "Can not activate the CURRENT component. If needed simply use hold method."
            // technically we could use suspend here , but since activate is used
            // outside of process definitions we don't want to overcomplete the API for this
            // rare edge case
            // workaround yield(activate(process = Component::process))
        }

        var p: ProcessPointer? = null

        if(process == null) {
            if(status == DATA) {
                //                require(this.simProcess != null) { "no process for data component" }
                // note: not applicable, because the test would be if Component has a method called process, which it does by design

                p = Component::process
            }
        } else {
            p = process
        }

        var extra = ""

        if(p != null) {
            this.simProcess = ingestFunPointer(p)

            extra = "process=${p.name}"
        }

        if(status != CURRENT) {
            remove()
            if(p != null) {
                if(!(keepRequest || keepWait)) {
                    checkFail()
                }
            } else {
                checkFail()
            }
        }

        val scheduledTime = if(at == null) {
            env.now + delay.toDouble()
        } else {
            at.toDouble() + delay.toDouble()
        }

        reschedule(scheduledTime, priority, urgent, null, "activate $extra", SCHEDULED)

        return this
    }

    internal fun checkFail() {
        if(requests.isNotEmpty()) {
            log("request failed")
            requests.forEach { it.key.removeRequester(this) }
            requests.clear()
            failed = true
        }

        if(waits.isNotEmpty()) {
            log("wait failed")
            waits.forEach { it.state.waiters.remove(this) }

            waits.clear()
            failed = true
        }
    }

// TODO v0.4 reenable
//     fun hold(
//        duration: Number? = null,
//        till: Number? = null,
//        priority: Int = 0,
//        urgent: Boolean = false
//    ) {}

    /**
     * Hold the component.
     *
     * For `hold` contract see [user manual](https://www.kalasim.org/component/#hold)
     *
     * @param duration Time to hold. Either `duration` or `till` must be specified.
     * @param till Absolute time until the component should be held
     * @param priority If a component has the same time on the event list, this component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    suspend fun SequenceScope<Component>.hold(
        duration: Number? = null,
        till: Number? = null,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        description: String? = null
    ) = yieldCurrent {
        this@Component.hold(duration, till, priority, urgent, description)
    }

    /**
     * Hold the component.
     *
     * For `hold` contract see [user manual](https://www.kalasim.org/component/#hold)
     *
     * @param duration Time to hold. Either `duration` or `till` must be specified.
     * @param till Absolute time until the component should be held
     * @param priority If a component has the same time on the event list, this component is sorted according to
     * the priority. An event with a higher priority will be scheduled first.
     */
    fun hold(
        duration: Number? = null,
        till: Number? = null,
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        description: String? = null
    ) {
        if(status != PASSIVE && status != CURRENT) {
            requireNotData()
            remove()
            checkFail()
        }

        val scheduledTime = env.calcScheduleTime(till, duration)

        reschedule(scheduledTime, priority, urgent, description, "hold", SCHEDULED)
    }


    fun getThis() = this

    fun callProcess() = simProcess!!.call()

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
    fun release(resource: Resource, quantity: Double = Double.MAX_VALUE) = release(ResourceRequest(resource, quantity))


    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.
     */
    fun release(vararg resources: Resource) =
        release(*resources.map { it withQuantity Double.MAX_VALUE }.toTypedArray())


    /**
     * Release a quantity from a resource or resources.
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
            require(!resource.anonymous) { " It is not possible to release from an anonymous resource, this way. Use Resource.release() in that case." }

            releaseInternal(resource, quantity)
        }

        if(releaseRequests.isEmpty()) {
            log("Releasing all claimed resources ${claims}")

            for((r, _) in claims) {
                releaseInternal(r)
            }
        }
    }

    // todo move this function into Resource
    private fun releaseInternal(resource: Resource, q: Double? = null, bumpedBy: Component? = null) {
        require(resource in claims) { "$name not claiming from resource ${resource.name}" }

        val quantity = if(q == null) {
            claims[resource]!!
        } else if(q > claims[resource]!!) {
            claims[resource]!!
        } else {
            q
        }

        resource.claimed -= quantity

        log(
            ResourceEvent(
                env.now,
                this,
                resource,
                env.curComponent,
                RELEASED,
                quantity,
                resource.capacity,
                resource.claimed
            )
        )

        claims[resource] = claims[resource]!! - quantity

        if(claims[resource]!! < EPS) {
            leave(resource.claimers)
            claims.remove(resource)
        }

        // check for rounding errors salabim.py:12290
        require(!resource.claimers.isEmpty() || resource.claimed == 0.0) { "rounding error in claimed quantity" }
        // fix if(claimers.isEmpty()) field= 0.0

        if(bumpedBy == null) resource.tryRequest()
    }


    /**
     * Wait for any or all of the given state values are met
     *
     * For `wait` contract see [user manual](https://www.kalasim.org/component/#wait)
     *
     * @sample TODO
     *
     * @param state state variable
     * @param waitFor State value to wait for
     * @param failAt If the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  If the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     */
// todo states may have different types so this methods does not make real sense here.
//  Either remove type from state or enforce the user to call wait multiple times
    suspend fun <T> SequenceScope<Component>.wait(
        state: State<T>,
        waitFor: T,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null
    ) = wait(
        StateRequest(state) { state.value == waitFor },
//        *states.map { StateRequest(it) }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
    )


    /**
     * Wait for any or all of the given state values are met
     *
     * For `wait` contract see [user manual](https://www.kalasim.org/component/#wait)
     *
     * @sample TODO
     *
     * @param stateRequests Requests indicating a state and a target condition or predicate for fulfilment
     * @param failAt If the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     * @param failDelay  If the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.
     * @param priority If a component has the same time on the event list, this component is sorted according to the priority. An event with a higher priority will be scheduled first.
     */
    suspend fun SequenceScope<Component>.wait(
        vararg stateRequests: StateRequest<*>,
        //todo change to support distribution parameters instead
        priority: Priority = NORMAL,
        urgent: Boolean = false,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        all: Boolean = false
    ) = yieldCurrent {
        if(status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        waitAll = all
        scheduledTime = env.now + (failAt?.sample() ?: Double.MAX_VALUE) + (failDelay?.sample() ?: 0.0)

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
                priority = priority,
                urgent = urgent,
                caller = "wait",
                newStatus = WAITING
            )
        }
    }

    internal fun tryWait(): Boolean {
        if(status == INTERRUPTED) {
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

            reschedule(env.now, NORMAL, false, null, "wait", SCHEDULED)
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
        (q as ComponentQueue<Component>).remove(this)
    }


    public override val info: Jsonable
        get() = ComponentInfo(this)


    private suspend fun SequenceScope<Component>.yieldCurrent(builder: () -> Unit = {}) {
        val initialStatus = status

        builder()

        if(initialStatus == CURRENT) {
            yield(this@Component)
        }
    }

    //redeclare to simplify imports
    @OptIn(KoinApiExtension::class)
    inline fun <reified T : Any> KoinComponent.get(
        qualifier: Qualifier? = null,
        noinline parameters: ParametersDefinition? = null
    ): T =
        getKoin().get(qualifier, parameters)


    internal fun requestedQuantity(resource: Resource) = requests[resource]

    /** Captures the current state of a `State`*/
    open class ComponentInfo(c: Component) : Jsonable() {
        val name = c.name
        val status = c.status
        val creationTime = c.creationTime
        val scheduledTime = c.scheduledTime

        val claims = c.claims.map { it.key.name to it.value }.toMap()
        val requests = c.requests.map { it.key.name to it.value }.toMap()
    }


    suspend fun SequenceScope<Component>.selectResource(
        resources: List<Resource>,
        quantity: Number = 1,
        policy: ResourceSelectionPolicy = RANDOM
    ): Resource {
        require(resources.isNotEmpty()) { "Resources listing must not be empty" }

        val selected = when(policy) {
            SHORTEST_QUEUE -> {
                resources.minByOrNull { it.requesters.size }!!
            }
            ROUND_ROBIN -> {
                // note could also be achieved with listOf<Resource>().repeat().iterator()
                val mapKey = listOf(this.hashCode(), resources.map { it.name }.hashCode()).hashCode()
                // initialize if not yet done
                val curValue = SELECT_SCOPE_IDX.putIfAbsent(mapKey, 0) ?: 0

                // increment for future calls
                SELECT_SCOPE_IDX.put(mapKey, (curValue + 1).rem(resources.size))

                return resources[curValue]
            }
            FIRST_AVAILABLE -> {
                while(resources.all { it.availableQuantity < quantity.toDouble() }) {
                    standby()
                }

                resources.first { it.availableQuantity > quantity.toDouble() }
            }
            RANDOM -> {
                resources[discreteUniform(0, resources.size - 1).sample()]
            }
            RANDOM_AVAILABLE -> {
                val available = resources.filter { it.availableQuantity >= quantity.toDouble() }
                require(available.isNotEmpty()) { "Not all resources must be in use to use RANDOM_AVAILABE selection policy" }

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
    suspend fun <T : Component> SequenceScope<Component>.batch(
        queue: ComponentQueue<T>,
        batchSize: Int,
        timeout: Int? = null
    ): List<T> {
        // Note: Adopted from simmer::batch (Ucar2019, p14)

        require(batchSize > 0) { "Batch size must be positive" }

        val queueListener = object : QueueChangeListener<T>() {
            override fun added(component: T) {
                if(queue.size >= batchSize) {
                    activate()
                }
            }
        }

        if(queue.size < batchSize) {
            queue.addChangeListener(queueListener)
            hold(timeout)
        }

        val actualBatchSize = min(batchSize, queue.size)
        val batch = List(actualBatchSize) { queue.poll() }

        queue.removeChangeListener(queueListener)

        return batch
    }
}

internal val SELECT_SCOPE_IDX = mutableMapOf<Int, Int>()


enum class ResourceSelectionPolicy {
    SHORTEST_QUEUE, FIRST_AVAILABLE, RANDOM, RANDOM_AVAILABLE, ROUND_ROBIN
}


//
// Abstract component process to be either generator or simple function
//

typealias ProcessPointer = KFunction1<*, Sequence<Component>>
//typealias GenProcess = KFunction1<*, Sequence<Component>>


interface SimProcess {
    fun call()

    val name: String
}

class GenProcessInternal(val component: Component, seq: Sequence<Component>, override val name: String) : SimProcess {

    val iterator = seq.iterator()

    override fun call() {
        try {
            iterator.next()
        } catch(e: NoSuchElementException) {
            component.terminate()
        }
    }
}

class SimpleProcessInternal(val component: Component, val funPointer: ProcessPointer, override val name: String) :
    SimProcess {
    override fun call() {
        funPointer.call(component)
    }
}

internal const val DEFAULT_REQUEST_QUANTITY = 1.0

data class ResourceRequest(val r: Resource, val quantity: Double = DEFAULT_REQUEST_QUANTITY, val priority: Priority? = null)

infix fun Resource.withQuantity(quantity: Number) = ResourceRequest(this, quantity.toDouble())
infix fun Resource.withPriority(priority: Int) = ResourceRequest(this, priority = Priority(priority))

infix fun ResourceRequest.andPriority(priority: Priority) = ResourceRequest(this.r, this.quantity, priority)

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

private fun formatWithInf(time: Double) =
    if(time == Double.MAX_VALUE || time.isInfinite()) "<inf>" else TRACE_DF.format(time)


data class ComponentLifecycleRecord(
    val component: String,
    val createdAt: Double,
    val inDataSince: Double?,
    val inData: Double,
    val inCurrent: Double,
    val inStandby: Double,
    val inPassive: Double,
    val inInterrupted: Double,
    val inScheduled: Double,
    val inRequesting: Double,
    val inWaiting: Double
)

fun Component.toLifeCycleRecord(): ComponentLifecycleRecord {
    val c = this

    val histogram: Map<ComponentState, Double> = c.statusMonitor.summed()

    return ComponentLifecycleRecord(
        c.name,
        c.creationTime,
        inDataSince = if(c.isData) c.statusMonitor.statsData().timepoints.last() else null,
        histogram.get(DATA) ?: 0.0,
        histogram[CURRENT] ?: 0.0,
        histogram[STANDBY] ?: 0.0,
        histogram[PASSIVE] ?: 0.0,
        histogram[INTERRUPTED] ?: 0.0,
        histogram[SCHEDULED] ?: 0.0,
        histogram[REQUESTING] ?: 0.0,
        histogram[WAITING] ?: 0.0,
    )
}
