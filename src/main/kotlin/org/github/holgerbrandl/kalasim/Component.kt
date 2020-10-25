package org.github.holgerbrandl.kalasim

import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.RealDistribution
import org.github.holgerbrandl.kalasim.ComponentState.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1


/**
 * A salabim component is used as component (primarily for queueing)
or as a component with a process
Usually, a component will be defined as a subclass of Component.
 *
 * @param process  of process to be started.  if None (default), it will try to start self.process()
 * @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)

 */
open class Component(
    name: String? = null,
    process: FunPointer? = Component::process,
    val priority: Int = 0,
    val delay: Int = 0
) : KoinComponent {

    protected val env: Environment by inject()

    var name: String
        private set

    private val requests = mapOf<Resource, Double>().toMutableMap()
    private val waits = listOf<StateRequest<*>>().toMutableList()
    val claims = mapOf<Resource, Double>().toMutableMap()

    private var failed: Boolean = false
    private var waitAll: Boolean = false

    private var process: SimProcess? = null


    var scheduledTime = Double.MAX_VALUE

    private var remainingDuration = 0.0

    var status: ComponentState = DATA

    init {
        this.name = nameOrDefault(name)

        val dataSuffix = if (process == null && this.name != MAIN) " data" else ""
        env.addComponent(this)
        env.printTrace(now(), env.curComponent, this, "create" + dataSuffix)


        // if its a generator treat it as such
        this.process = ingestFunPointer(process)

        if (process != null) {
            scheduledTime = env.now + delay

            reschedule(scheduledTime, priority, false, "activate", extra = "TODO")
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
                val sequence = process.call(this) as Sequence<Component>
                GenProcessInternal(this, sequence)
            } else {
                SimpleProcessInternal(this, process)
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

    open fun process() = this.let {
        sequence {
//            while (true) { // disabled because too much abstraction
            process(it)
            process()
//            }
        }
    }

    /** Generator function that implements "process". This can be overwritten in component classes a convenience alternative to process itself.*/
    open suspend fun SequenceScope<Component>.process(it: Component) {}

    open suspend fun SequenceScope<Component>.process() {}


//    open suspend fun SequenceScope<Component>.process() {
//        while (true)
//            yield(hold(1.0))
//    }

    val isPassive: Boolean
        get() = status == PASSIVE

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
            //todo checkFail
            remainingDuration = scheduledTime - env.now
        }

        scheduledTime = Double.MAX_VALUE
        status = PASSIVE
        env.printTrace(now(), env.curComponent, this)


        return this
    }


    /**
     * cancel component (makes the component data)
     *
     * See https://www.salabim.org/manual/Component.html#cancel
     */
    fun cancel() {
        if (status != CURRENT) {
            requireNotData()
            remove()
            //todo checkFail
        }

        process = null
        scheduledTime = Double.MAX_VALUE

        status = DATA

        env.printTrace(now(), env.curComponent, this, "cancel")
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
        env.addStandy(this)

        status = STANDBY
        env.printTrace(now(), env.curComponent, this)

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

     * @sample org.github.holgerbrandl.kalasim.examples.kalasim.GasStation.main
     */
    fun request(
        vararg resources: Resource,
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
    fun fixed(value: Double) = value.asConstantDist()
    fun Double.asConstantDist() = ConstantRealDistribution(this)


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
     * @sample org.github.holgerbrandl.kalasim.examples.kalasim.GasStation.main
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
        //todo change to support distribution parameters instead
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        oneOf: Boolean = false,
        //todo use type here and not string
        // try to avoid argument by inferring from stacktrace
        calledFrom:String? = null

    ): Component {

        //todo oneof_request

        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        scheduledTime = failAt?.sample() ?: Double.MAX_VALUE
        failed = false

//        val rr = resourceRequests.first()
        resourceRequests.forEach { (r, quantity, priority) ->
            var q = 1 -quantity


            if (r.isPreemptive && resourceRequests.size > 1) {
                throw IllegalArgumentException("preemptive resources do not support multiple resource requests")
            }


            // TODO
            if(calledFrom == "put"){
                q = -q
            }

            require(q > 0 || r.anonymous){ "quantity <0" }

            //  is same resource is specified several times, just add them up
            //https://stackoverflow.com/questions/53826903/increase-value-in-mutable-map
            requests.merge(r, q, Double::plus)


            val reqText = (calledFrom ?: "") + "request ${q} from ${r.name}  with priority ${priority} and oneof=${oneOf}"


//            enterSorted(r.requesters, priority)
            r.requesters.add(this, priority)

            env.printTrace(
                now(),
                env.curComponent,
                this,
                REQUESTING.toString() + " " + r.name
            )

            // TODO build test case (current implementation seems incorrect & incomplete
            if (r.isPreemptive) {
                var av = r.availableQuantity()
                val thisClaimers = r.claimers.q

                var bumpCandidates= mutableListOf<Component>()
//                val claimComponents = thisClaimers.map { it.c }
                for (cqe in thisClaimers.reversed() ) {
                    if (av >= q) {
                        break
                    }

                    // check if prior of component
                    if (priority !=null && priority >= (thisClaimers.find { it.c ==  this }?.priority ?: Int.MIN_VALUE)) {
                        break
                    }

                    av += quantity
                    bumpCandidates.add(cqe.c)
                }

                if(av >=0){
                    bumpCandidates.forEach{
                        it.releaseInternal(r)
                        env.printTrace("$it bumped from $r by $this")
                        it.activate()
                    }
                }
            }
        }


        requests.forEach{ (resource, quantity) ->
            if(quantity< resource.minq)
                resource.minq= quantity
        }

        tryRequest()

        if (requests.isNotEmpty()) {
            reschedule(scheduledTime, 0, false, "request")
        }

        return this
    }

    private fun tryRequest() {
        TODO("Not yet implemented")
    }

    @Deprecated("no longer needed, handled by queue directly")
    private fun enterSorted(requesters: Queue<Component>, priority: Int) {
        TODO("Not yet implemented")
    }


    private fun remove() {
        val queueElem = env.eventQueue.firstOrNull {
            it.component == this
        }

        if (queueElem != null) {
            env.eventQueue.remove(queueElem)
        }

        if (status == STANDBY) {
            env.addStandy(this)
            env.addPendingStandBy(this)
        }
    }

    fun terminate(): Component {
        claims.forEach { (resource, quantity) ->
            resource.release(quantity)
        }

        status = DATA
        scheduledTime = Double.MAX_VALUE
        process = null

        env.printTrace(now(), env.curComponent, this, "ended")

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
        extra: Any? = null
    ) {
        require(scheduledTime >= env.now) { "scheduled time (${scheduledTime}) before now (${env.now})" }

        this.scheduledTime = scheduledTime

        if (this.scheduledTime != Double.MAX_VALUE) push(this.scheduledTime, priority, urgent)

        //todo implement extra
        val extra = ""

        // calculate scheduling delta
        val delta = if (this.scheduledTime == env.now || (this.scheduledTime == Double.MAX_VALUE)) "" else {
            "+" + (this.scheduledTime - env.now)
        }

        // print trace
        env.printTrace(now(), env.curComponent, this, "$caller $delta $extra")
    }

    /**
     * Activate component
     *
     * See https://www.salabim.org/manual/Component.html#activate
     *
     * @param process name of process to be started.
     * * if None (default), process will not be changed
     * * if the component is a data component, the
     * * generator function process will be used as the default process.
     * * note that the function *must* be a generator, i.e. contains at least one yield.
     */
    fun activate(
        at: Double? = null,
        priority: Int = 0,
        keepRequest: Boolean = false,
        keepWait: Boolean = false,
        urgent: Boolean = false,
        process: FunPointer? = null
    ): Component {

        val p = if (process == null) {
            if (status == DATA) require(this.process != null) { "no process for data component" }
            this.process
        } else {
            ingestFunPointer(process)
        }

        var extra = ""

        if (p != null) {
            this.process = p

            extra = "process ${process}"
        }
        //todo
//            if inspect.isgeneratorfunction(p):
//            self._process = p(**kwargs)
//            self._process_isgenerator = True
//            else:
//            self._process = p
//            self._process_isgenerator = False
//            self._process_kwargs = kwargs


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
            env.now + delay
        } else {
            at + delay
        }

        reschedule(scheduledTime, priority, urgent, "hold", extra)

        return (this)
    }

    private fun checkFail() {
        if (requests.isNotEmpty()) {
            env.printTrace("request failed")
            requests.clear()
        }
        if (waits.isNotEmpty()) {
            env.printTrace("request failed")
            waits.clear()
        }

        TODO("Not yet implemented")
//        if(requests.isNotEmpty()){
//            requests.forEach{ leave()}
//        }
//        wai
//    wai
    }


    /**
     * hold the component. See https://www.salabim.org/manual/Component.html#hold
     */
    fun hold(
        duration: Double? = null,
        till: Double? = null,
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

        reschedule(scheduledTime, priority, urgent, "hold")

        return (this)
    }


    var seq: Int = 0

    private fun push(scheduledTime: Double, priority: Int, urgent: Boolean) {
        seq++
        val heapSeq = if (urgent) -seq else seq

//        https://bezkoder.com/kotlin-priority-queue/
        env.eventQueue.add(QueueElement(scheduledTime, priority, heapSeq, this))
    }

    override fun toString(): String {
        //todo implement bits from print_info
        return "todo implement bits from print_info" + super.toString()
    }

    fun callProcess() = process!!.call()

    /**
     *   release a quantity from a resource or resources
     *
     * It is not possible to release from an anonymous resource, this way.
    Use Resource.release() in that case.
     *
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(resource: Resource) {
        require(resource.anonymous == false) { " It is not possible to release from an anonymous resource, this way.            Use Resource.release() in that case." }

        // TODO Incomplete implementation in case of arguments

        if (claims.containsKey(resource)) {
            releaseInternal(resource)
        }

    }

    private fun releaseInternal(resource: Resource, q: Double? = null) {
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

        if (claims[resource]!! < 1E-8) {
            leave(resource.claimers)
        }
    }

    // todo states may have different types so this methods does not make real sense here. Either remove type from state or enforce the user to call wait multiple times
    fun <T> wait(
        state: State<T>,
        value: T,
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null
    ): Component = wait(
        StateRequest(state, { state.value == value }),
//        *states.map { StateRequest(it) }.toTypedArray(),
        failAt = failAt,
        failDelay = failDelay,
    )


    /**
     * Wait for any or all of the given state values are met
     *
     * @sample TODO
     *
     * @param args Sequence of items, where each item can be
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
        failAt: RealDistribution? = null,
        failDelay: RealDistribution? = null,
        oneOf: Boolean = false
    ): Component {

        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            checkFail()
        }

        scheduledTime = env.now +
                (failAt?.sample() ?: Double.MAX_VALUE) +
                (failDelay?.sample() ?: 0.0)


        stateRequests
            // skip already tracked states
            .filterNot { sr -> waits.any { it.state == sr.state } }
            .forEach { (state, _, priority) ->
                    state.addWaiter(this, priority)
            }

        tryWait()

        if (waits.isNotEmpty()) {
            reschedule(scheduledTime, 0, false, "wait")
        }

        return this
    }

    private fun tryWait(): Boolean {
        if (status == INTERRUPTED) {
            return false
        }

        val honored = if(waitAll) {
            waits.all { sr ->
                (sr as StateRequest<Any>).predicate(sr.state as State<Any>)
            }
        }else{
            waits.any { sr ->
                (sr as StateRequest<Any>).predicate(sr.state as State<Any>)
            }
        }

        if(honored){
            waits.forEach { sr-> sr.state.removeWaiter(this)}
            waits.clear()
            reschedule(scheduledTime, 0, false, "wait")
        }

        return honored
    }

    /**
     * Leave queue
     *
     * @param q Queue queue to leave
     */
    fun leave(q: ComponentQueue<Component>) {
        env.printTrace("leave ${q.name}")
        q.remove(this)
    }
}



typealias FunPointer = KFunction<*>
typealias GenProcess = KFunction1<*, Sequence<Component>>


interface SimProcess {
    fun call()
}

class GenProcessInternal(val component: Component, seq: Sequence<Component>) : SimProcess {

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

class SimpleProcessInternal(val component: Component, val funPointer: FunPointer) : SimProcess {
    override fun call() {
        funPointer.call(component)
    }
}


data class QueueElement(val time: Double, val priority: Int, val seq: Int, val component: Component) :
    Comparable<QueueElement> {
    override fun compareTo(other: QueueElement): Int {
        return compareValuesBy(this, other, { it.priority }, { it.time })
    }

    override fun toString(): String {
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
    }
}

enum class ComponentState {
    DATA, CURRENT, STANDBY, PASSIVE, INTERRUPTED, SCHEDULED, REQUESTING, WAITING
}

data class ResourceRequest(val r: Resource, val quantity: Double, val priority: Int? = null)

fun main() {
    val (state, predicate, priority) = StateRequest(State("foo"), { it.value == "House" })
    predicate(state)
}

//    data class StateRequest<T>(val s: State<T>, val value: T? = null, val priority: Int? = null)
data class StateRequest<T>(val state: State<T>, val predicate: (State<T>) -> Boolean, val priority: Int? = null) {
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


infix fun Resource.withQuantity(quantity: Double) = ResourceRequest(this, quantity)