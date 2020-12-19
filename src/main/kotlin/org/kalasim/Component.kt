package org.kalasim

import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.kalasim.ComponentState.*
import org.kalasim.misc.Jsonable
import org.kalasim.misc.TRACE_DF
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import java.util.*
import kotlin.reflect.KFunction1


internal val EPS = 1E-8

typealias ProcContext = SequenceScope<Component>

enum class ComponentState {
    DATA, CURRENT, STANDBY, PASSIVE, INTERRUPTED, SCHEDULED, REQUESTING, WAITING
}


/**
 * A kalasim component is used as component (primarily for queueing) or as a component with a process.
 * Usually, a component will be defined as a subclass of Component.
 *
 * @param at schedule time
 * @param delay schedule with a delay if omitted, no delay
 * @param process  of process to be started.  if None (default), it will try to start self.process()
 * @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)
 */
open class Component(
    name: String? = null,
    process: FunPointer? = Component::process,
    val priority: Int = 0,
    delay: Number = 0,
    koin : Koin = GlobalContext.get()
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


    // **{todo}** 0.6 get rid of this field (not needed because can be always retrieved from eventList if needed
    var scheduledTime = Double.MAX_VALUE

    private var remainingDuration = 0.0

    var status: ComponentState = DATA
        set(value) {
            field = value
            statusMonitor.addValue(value)
        }

    val statusMonitor = FrequencyLevelMonitor<ComponentState>(status, "status of ${name}", koin)


    init {
        val dataSuffix = if (process == null && this.name != MAIN) " data" else ""
        env.addComponent(this)
        printTrace(now(), env.curComponent, this, "create", dataSuffix)


        // if its a generator treat it as such
        this.simProcess = ingestFunPointer(process)

        if (process != null) {
            scheduledTime = env.now + delay.toDouble()

            reschedule(scheduledTime, priority, false, "activate", SCHEDULED)
        }

        @Suppress("LeakingThis")
        setup()
    }


    private fun ingestFunPointer(process: FunPointer?): SimProcess? {
//        if(process != null ){
//            print("param type is " + process!!.returnType)
//            if(process!!.returnType.toString().startsWith("kotlin.sequences.Sequence"))
//        }

        return if (process != null) {
            val isGenerator = process.returnType.toString().startsWith("kotlin.sequences.Sequence")

            if (isGenerator) {
                @Suppress("UNCHECKED_CAST")
                val sequence = process.call(this)
                GenProcessInternal(this, sequence, process.name)
            } else {
                SimpleProcessInternal(this, process, process.name)
            }
        } else {
            null
        }
    }

    /**  called immediately after initialization of a component.
     * by default this is a dummy method, but it can be overridden.
     * */
    open fun setup() {}

    /**         the current simulation time : float */
    fun now() = env.now
//    fun now() = env.now()

    val now: Double
        get() = now()

    open fun process() = this.let {
        sequence {
//            while (true) { // disabled because too much abstraction
            process(it)
            process()
//            }
        }
    }

    /** Generator function that implements "process". This can be overwritten in component classes a convenience alternative to process itself.*/
    open suspend fun ProcContext.process(it: Component) {}

    open suspend fun ProcContext.process() {}


//    open suspend fun SequenceScope<Component>.process() {
//        while (true)
//            yield(hold(1.0))
//    }

    /** @return `true` if status is `PASSIVE`, `false` otherwise. */
    val isPassive: Boolean
        get() = status == PASSIVE

    /** @return `true` if status is `CURRENT`, `false` otherwise. */
    val isCurrent: Boolean
        get() = status == CURRENT

    /** @return `true` if status is `STANDBY`, `false` otherwise. */
    val isStandby: Boolean
        get() = status == STANDBY

    /** @return `true` if status is `INTERRUPTED`, `false` otherwise. */
    val isInterrrupted: Boolean
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


    /** Passivate a component
     *
     * See https://www.salabim.org/manual/Component.html#passivate
     */
    fun passivate(): Component {
        if (status == CURRENT) {
            remainingDuration = 0.0
        } else {
            requireNotData()
            remove()
            checkFail()
            remainingDuration = scheduledTime - env.now
        }


        scheduledTime = Double.MAX_VALUE
        status = PASSIVE
        printTrace(now(), env.curComponent, this, "passivate")


        return this
    }

    /** interrupt level of an interrupted component  non interrupted components return 0. */
    var interruptLevel = 0
        private set

    /** Interrupt the component. */
    fun interrupt() {
        require(status != CURRENT) { "Current component can no be interrupted" }

        if (status == INTERRUPTED) {
            interruptLevel++
        } else {
            requireNotData()
            remove()
            remainingDuration = scheduledTime - env.now
            interruptLevel = 1
            status = INTERRUPTED
        }

        printTrace("interrupt (level=$interruptLevel)")
    }

    /** resumes an interrupted component
     * @param if `true`, the component returns to the original status, regardless of the number of interrupt levels if
     * `false` (default), the interrupt level will be decremented and if the level reaches 0, the component will return
     * to the original status.
     * @param if a component has the same time on the event list, this component is sorted accoring to
    the priority.
     */
    fun resume(all: Boolean = false, priority: Int = 0) {
        // not part of original impl
        require(status == INTERRUPTED)
        require(interruptLevel <0)


        interruptLevel--




    }

    /**
     * cancel component (makes the component data)
     *
     * See https://www.salabim.org/manual/Component.html#cancel
     */
    fun cancel(): Component {
        if (status != CURRENT) {
            requireNotData()
            remove()
            checkFail()
        }

        simProcess = null
        scheduledTime = Double.MAX_VALUE

        status = DATA

        printTrace(now(), env.curComponent, this, null, "cancel")

        return this
    }

    /**
     * puts the component in standby mode.
     *
     * Not allowed for data components or main.
     *
     * See https://www.salabim.org/manual/Component.html#standby
     */
    fun standby() {
        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            //todo checkFail
        }

        scheduledTime = env.now
        env.addStandBy(this)

        status = STANDBY
        printTrace(now(), env.curComponent, this, null)
    }

    fun put(
        vararg resourceRequests: ResourceRequest,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
    ): Component = request(
        *resourceRequests.map { it.copy(quantity = -it.quantity) }.toTypedArray(),
        failAt = failAt, failDelay = failDelay
    )

    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.

     * @sample org.kalasim.examples.Refuel.main
     */
    fun request(
        resources: Collection<Resource>,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        oneOf: Boolean = false,
    ) = request(
        *resources.map { it withQuantity 1.0 }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf
    )

    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.
     *
     * @sample org.kalasim.examples.Refuel.main
     */
    fun request(
        vararg resources: Resource,
        // todo review if this should rather be a number (and dist at call site)
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        oneOf: Boolean = false,
    ) = request(
        *resources.map { it withQuantity 1.0 }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
        oneOf = oneOf
    )


    //todo we should just support one here
//    fun fixed(value: Double) = value.asConstantDist()
//
    @Suppress("unused")
    fun Number.asConstantDist() = ConstantRealDistribution(this.toDouble())

    fun Number.asDist() = ConstantRealDistribution(this.toDouble())

    fun fixed(value: Double) = ConstantRealDistribution(value)

    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.

     * Examples
     * - `request(r1)` --> requests 1 from r1
     * - `request(r1,r2)` --> requests 1 from r1 and 1 from r2
     * - `request(r1,(r2,2),(r3,3,100))` --> requests 1 from r1, 2 from r2 and 3 from r3 with priority 100
     * - `request((r1,1),(r2,2))` --> requests 1 from r1, 2 from r2
     * - `request(r1, r2, r3, oneoff=True)` --> requests 1 from r1, r2 or r3
     *
     *   `request` has the effect that the component will check whether the requested quantity from a resource is
     *   available. It is possible to check for multiple availability of a certain quantity from several resources.
     *
     *  Not allowed for data components or main.
     *
     * If to be used for the current component
     * (which will be nearly always the case),
     * use `yield (request(...))`.
     *
     * If the same resource is specified more that once, the quantities are summed
     *
     * The requested quantity may exceed the current capacity of a resource
     *
     * The parameter failed will be reset by a calling request or wait
     *
     * @sample org.kalasim.examples.Refuel.main
     *
     * @param resourceRequests sequence of items where each item can be:
     * - resource, where quantity=1, priority=tail of requesters queue
     * - tuples/list containing a resource, a quantity and optionally a priority. if the priority is not specified,
     * the request for the resource be added to the tail of the requesters queue
     *
     * @param failAt if the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     *
     * @param failDelay  if the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.

    @param oneOf If `true`, just one of the requests has to be met (or condition),
    where honoring follows the order given
     */
    fun request(
        vararg resourceRequests: ResourceRequest,
        priority: Int = 0,
        urgent: Boolean = false,
        //todo change to support distribution parameters instead
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        oneOf: Boolean = false,
        //todo use type here and not string
        // try to avoid argument by inferring from stacktrace
        calledFrom: String? = null

    ): Component {

        //todo oneof_request

        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        scheduledTime = env.now +
                (failAt?.sample() ?: Double.MAX_VALUE) +
                (failDelay?.sample() ?: 0.0)

        failed = false
        this.oneOfRequest = oneOf

//        val rr = resourceRequests.first()
        resourceRequests.forEach { (r, quantity, priority) ->
            var q = quantity

            if (r.preemptive && resourceRequests.size > 1) {
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
                (calledFrom ?: "") + "requesting ${q} from ${r.name} with priority ${priority} and oneof=${oneOf}"

//            enterSorted(r.requesters, priority)
            r.requesters.add(this, priority)

            printTrace(
                now(),
                env.curComponent,
                this,
//                REQUESTING.toString() + " " + r.name
                null,
                reqText
            )

            // TODO build test case (current implementation seems incorrect & incomplete
            if (r.preemptive) {
                var av = r.availableQuantity
                val thisClaimers = r.claimers.q

                val bumpCandidates = mutableListOf<Component>()
//                val claimComponents = thisClaimers.map { it.c }
                for (cqe in thisClaimers.reversed()) {
                    if (av >= q) {
                        break
                    }

                    // check if prior of component
                    if (priority != null && priority >= (thisClaimers.find { it.component == this }?.priority
                            ?: Int.MIN_VALUE)
                    ) {
                        break
                    }

                    av += quantity
                    bumpCandidates.add(cqe.component)
                }

                if (av >= 0) {
                    bumpCandidates.forEach {
                        it.releaseInternal(r, bumpedBy = this)
                        printTrace("$it bumped from $r by $this")
                        it.activate()
                    }
                }
            }
        }

        requests.forEach { (resource, quantity) ->
            if (quantity < resource.minq)
                resource.minq = quantity
        }

        tryRequest()

        if (requests.isNotEmpty()) {
            reschedule(scheduledTime, priority, urgent, "request", REQUESTING)
        }

        return this
    }


    // TODO what is the reasoning here
    private fun honorAll(): List<Pair<Resource, Double>>? {
        for ((r, requestedQuantity) in requests) {
            if (requestedQuantity > r.capacity - r.claimedQuantity + EPS) {
                return null
            } else if (-requestedQuantity > r.claimedQuantity + EPS) {
                return null
            }
        }

        return requests.toList()
    }

    private fun honorAny(): List<Pair<Resource, Double>>? {
        for (request in requests) {
            val (r, requestedQuantity) = request
            if (requestedQuantity > r.capacity - r.claimedQuantity + EPS) {
                return listOf(r to requestedQuantity)
            } else if (-requestedQuantity <= r.claimedQuantity + EPS) {
                return listOf(r to requestedQuantity)
            }
        }

        return null
    }

    internal open fun tryRequest(): Boolean {
        if (status == INTERRUPTED) return false

        val rHonor = if (oneOfRequest) honorAny() else honorAll()

        if (rHonor.isNullOrEmpty()) return false

        requests
//            .filterNot { it.key.anonymous }
            .forEach { (resource, quantity) ->
                // proceed just if request was honored claim it
                if (rHonor.any { it.first == resource }) {
                    resource.claimedQuantity += quantity //this will also update the monitor

                    if (!resource.anonymous) {
                        val thisPrio = resource.requesters.q.firstOrNull { it.component == this }?.priority
                        claims.merge(resource, quantity, Double::plus)

                        //also register as claimer in resource if not yet present
                        if (resource.claimers.q.none { it.component == this }) {
                            resource.claimers.add(this, thisPrio)
                        }
                    }
                }

                resource.removeRequester(this)
            }

        requests.clear()
        remove()

        val honorInfo = rHonor.firstOrNull()!!.first.name + (if (rHonor.size > 1) "++" else "")

        reschedule(now(), 0, false, "request honor $honorInfo", SCHEDULED)

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
        return if (resource == null) {
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

    fun terminate(): Component {
        claims.forEach { (resource, quantity) ->
            resource.release(quantity)
        }

        status = DATA
        scheduledTime = Double.MAX_VALUE
        simProcess = null

        printTrace(now(), env.curComponent, this, null, "ended")

        return (this)
    }

    private fun requireNotData() =
        require(status != DATA) { name + "data component not allowed" }

    private fun requireNotMain() =
        require(this != env.main) { name + "data component not allowed" }

    fun reschedule(
        scheduledTime: Double,
        priority: Int = 0,
        urgent: Boolean = false,
        caller: String? = null,
        newStatus: ComponentState,
    ) {
        require(scheduledTime >= env.now) { "scheduled time (${scheduledTime}) before now (${env.now})" }
        require(this !in env.queue) { "component must not be in queue when reschudling but must be removed already at this point" }

        status = newStatus

        this.scheduledTime = scheduledTime

        if (this.scheduledTime != Double.MAX_VALUE) {
            env.push(this, this.scheduledTime, priority, urgent)
        }

        //todo implement extra
        val extra = "scheduled for ${TRACE_DF.format(scheduledTime)}"

        // line_no: reference to source position
        // 9+ --> continnue generator
        // 13 --> no plus means: generator start


        // calculate scheduling delta
        val delta = if (this.scheduledTime == env.now || (this.scheduledTime == Double.MAX_VALUE)) "" else {
            "+" + TRACE_DF.format(this.scheduledTime - env.now) + " "
        }

        // print trace
        printTrace(now(), env.curComponent, this, caller + delta, extra)
    }

    /**
     * Activate component
     *
     * See https://www.salabim.org/manual/Component.html#activate
     *
     * @param at schedule time
     * @param delay schedule with a delay if omitted, no delay
     * @param process name of process to be started.
     * * if None (default), process will not be changed
     * * if the component is a data component, the
     * * generator function process will be used as the default process.
     * * note that the function *must* be a generator, i.e. contains at least one yield.
     */
    fun activate(
        at: Number? = null,
        delay: Number = 0,
        priority: Int = 0,
        urgent: Boolean = false,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        process: FunPointer? = null
    ): Component {

        val p = if (process == null) {
            if (status == DATA) {
                require(this.simProcess != null) { "no process for data component" }
            }

            this.simProcess
        } else {
            ingestFunPointer(process)
        }

        var extra = ""

        if (p != null) {
            this.simProcess = p

            extra = "process=${p.name}"
        }

        if (status != CURRENT) {
            remove()
            if (p != null) {
                if (!(keepRequest || keepWait)) {
                    checkFail()
                }
            } else {
                checkFail()
            }
        }

        val scheduledTime = if (at == null) {
            env.now + delay.toDouble()
        } else {
            at.toDouble() + delay.toDouble()
        }

        reschedule(scheduledTime, priority, urgent, "activate $extra", SCHEDULED)

        return (this)
    }

    private fun checkFail() {
        if (requests.isNotEmpty()) {
            printTrace("request failed")
            requests.forEach { it.key.removeRequester(this) }
            requests.clear()
            failed = true
        }

        if (waits.isNotEmpty()) {
            printTrace("wait failed")
            waits.forEach { it.state.waiters.remove(this) }

            waits.clear()
            failed = true
        }
    }


    /**
     * hold the component. See https://www.salabim.org/manual/Component.html#hold
     */
    fun hold(
        duration: Number? = null,
        till: Number? = null,
        priority: Int = 0,
        urgent: Boolean = false
    ): Component {
        if (status != DATA && status != CURRENT) {
            requireNotData()
            remove()
            //todo
//            _check_fail()
        }

        val scheduledTime = env.calcScheduleTime(till, duration)

        reschedule(scheduledTime, priority, urgent, "hold", SCHEDULED)

        return (this)
    }


    fun callProcess() = simProcess!!.call()

    /**
     * Release a quantity from a resource or resources.
     *
     * It is not possible to release from an anonymous resource, this way.
     * Use Resource.release() in that case.
     *
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(resource: Resource, quantity: Double = Double.MAX_VALUE) = release(ResourceRequest(resource, quantity))


    /**
     * Request from a resource or resources
     *
     *  Not allowed for data components or main.
     */
    fun release(vararg resources: Resource) = release(*resources.map { it withQuantity 1.0 }.toTypedArray())


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
        for ((resource, quantity) in releaseRequests) {
            require(!resource.anonymous) { " It is not possible to release from an anonymous resource, this way. Use Resource.release() in that case." }

            releaseInternal(resource, quantity)
        }

        if (releaseRequests.isEmpty()) {
            printTrace("Releasing all claimed resources ${claims}")

            for ((r, _) in claims) {
                releaseInternal(r)
            }
        }
    }

    // todo move this function into Resource
    private fun releaseInternal(resource: Resource, q: Double? = null, bumpedBy: Component? = null) {
        require(resource in claims) { "$name not claiming from resource ${resource.name}" }

        val quantity = if (q == null) {
            claims[resource]!!
        } else if (q > claims[resource]!!) {
            claims[resource]!!
        } else {
            q
        }

        resource.claimedQuantity -= quantity

        claims[resource] = claims[resource]!! - quantity

        if (claims[resource]!! < EPS) {
            leave(resource.claimers)
            claims.remove(resource)
        }

        // check for rounding errors salabim.py:12290
        require(!resource.claimers.isEmpty() || resource.claimedQuantity == 0.0) { "rounding error in claimed quantity" }
        // fix if(claimers.isEmpty()) field= 0.0

        if (bumpedBy == null) resource.tryRequest()
    }


    // todo states may have different types so this methods does not make real sense here. Either remove type from state or enforce the user to call wait multiple times
    fun <T> wait(
        state: State<T>,
        waitFor: T,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null
    ): Component = wait(
        StateRequest(state) { state.value == waitFor },
//        *states.map { StateRequest(it) }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
    )


    /**
     * Wait for any or all of the given state values are met
     *
     * @sample TODO
     *
     * @param stateRequests Sequence of items, where each item can be
    - a state, where value=True, priority=tail of waiters queue)
    - a tuple/list containing
    state, a value and optionally a priority.
    if the priority is not specified, this component will
    be added to the tail of the waiters queue

     *
     * @param failAt If the request is not honored before fail_at, the request will be cancelled and the parameter failed will be set. If not specified, the request will not time out.
     *
     * @param failDelay  If the request is not honored before now+fail_delay,
    the request will be cancelled and the parameter failed will be set. if not specified, the request will not time out.

     */
    fun wait(
        vararg stateRequests: StateRequest<*>,
        //todo change to support distribution parameters instead
        priority: Int = 0,
        urgent: Boolean = false,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        all: Boolean = false
    ): Component {

        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        waitAll = all

        scheduledTime = env.now +
                (failAt?.sample() ?: Double.MAX_VALUE) +
                (failDelay?.sample() ?: 0.0)


        stateRequests
            // skip already tracked states
            .filterNot { sr -> waits.any { it.state == sr.state } }
            .forEach { sr ->
                val (state, srPriority, _) = sr
                state.waiters.add(this, srPriority)
                waits.add(sr)
            }

        tryWait()

        if (waits.isNotEmpty()) {
            reschedule(scheduledTime, priority, urgent, "wait", WAITING)
        }

        return this
    }

    internal fun tryWait(): Boolean {
        if (status == INTERRUPTED) {
            return false
        }

        @Suppress("UNCHECKED_CAST")
        val honored = if (waitAll) {
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


        if (honored) {
            waits.forEach { sr ->
                sr.state.waiters.remove(this)
            }
            waits.clear()
            remove()
            reschedule(now, 0, false, "wait", SCHEDULED)
        }

        return honored
    }

    /**
     * Leave queue
     *
     * @param q Queue queue to leave
     */
    fun leave(q: ComponentQueue<Component>) {
        printTrace("leave ${q.name}")
        q.remove(this)
    }


    public override val info: Jsonable
        get() = ComponentInfo(this)
}


/** Captures the current state of a `State`*/
open class ComponentInfo(c: Component) : Jsonable() {
    val name = c.name
    val status = c.status
    val creationTime = c.creationTime
    val scheduledTime = c.scheduledTime

    val claims = c.claims.toList()
    val requests = c.requests.toMap()
}

// todo clarify intent or remove
///** Captures the current state of a `State`*/
////@Serializable
//internal data class ComponentInfo2(val time: Double, val name: String, val value: String, val waiters: List<String>) {
//    override fun toString(): String {
////        return Json.encodeToString(this)
//        return GSON.toJson(this)
//    }
//}


//
// Abstract component process to be either generator or simple function
//

typealias FunPointer = KFunction1<*, Sequence<Component>>
typealias GenProcess = KFunction1<*, Sequence<Component>>


interface SimProcess {
    fun call()

    val name: String
}

class GenProcessInternal(val component: Component, seq: Sequence<Component>, override val name: String) : SimProcess {

    val iterator = seq.iterator()

    override fun call() {
        try {
            iterator.next()
        } catch (e: NoSuchElementException) {
            component.terminate()
        }

        //todo reenable
//        if(!iterator.hasNext()) {
//
//        }
    }
}

class SimpleProcessInternal(val component: Component, val funPointer: FunPointer, override val name: String) :
    SimProcess {
    override fun call() {
        funPointer.call(component)
    }
}

data class ResourceRequest(val r: Resource, val quantity: Double, val priority: Int? = null)

infix fun Resource.withQuantity(quantity: Number) = ResourceRequest(this, quantity.toDouble())

infix fun ResourceRequest.andPriority(priority: Int) = ResourceRequest(this.r, this.quantity, priority)

//    data class StateRequest<T>(val s: State<T>, val value: T? = null, val priority: Int? = null)
data class StateRequest<T>(val state: State<T>, val priority: Int? = null, val predicate: (T) -> Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StateRequest<*>

        if (state != other.state) return false
        if (predicate != other.predicate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + predicate.hashCode()
        return result
    }
}

infix fun <T> State<T>.turns(value: T) = StateRequest(this) { it == value }