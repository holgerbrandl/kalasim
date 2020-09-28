package org.github.holgerbrandl.basamil

import org.github.holgerbrandl.basamil.State.*
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1


private val componentCounters = mapOf<String, Int>().toMutableMap()

private fun getComponentCounter(className: String) = componentCounters.merge(className, 1, Int::plus)

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

/**
 * A salabim component is used as component (primarily for queueing)
or as a component with a process
Usually, a component will be defined as a subclass of Component.
 *
 * @param process  of process to be started.  if None (default), it will try to start self.process()
 * @param name name of the component.  if the name ends with a period (.), auto serializing will be applied  if the name end with a comma, auto serializing starting at 1 will be applied  if omitted, the name will be derived from the class it is defined in (lowercased)

 */
open class Component(
    protected val env: Environment, //todo inject via setter to simplify api
    name: String? = null,
    process: FunPointer? = Component::process,
    val priority: Int = 0,
    val delay: Int = 0
) {

    private val requests = mapOf<Resource, Int>().toMutableMap()
    val claims = mapOf<Resource, Int>().toMutableMap()

    private var failed: Boolean = false

    private var process: SimProcess? = null

    var name: String
        private set

    // make only getter private
    private var now = 0.0;
    var scheduledTime = Double.MAX_VALUE

    private var remainingDuration = 0.0

    var status: State = DATA

    init {

        //todo determine process
        this.name = name ?: javaClass.simpleName + "." + getComponentCounter(javaClass.simpleName)


        val dataSuffix = if (process == null && name() != MAIN) " data" else ""
        env.printTrace(now(), this, "create" + dataSuffix, "")


        // if its a generator treat it as such
        this.process = ingestFunPointer(process)

        if (process != null) {
            scheduledTime = env.now() + delay

            reschedule(scheduledTime, priority, false, "activate", extra = "TODO")
        }

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
    fun now() = now - env.offset
//    fun now() = env.now()

    open fun process(): Sequence<Component> = sequence {
        process()
    }

    open suspend fun SequenceScope<Component>.process() {
        while (true)
            yield(hold(1.0))
    }


    /** Passivate a component
     *
     * See https://www.salabim.org/manual/Component.html#passivate
     */
    fun passivate() {
        if (status == CURRENT) {
            remainingDuration = 0.0
        } else {
            requireNotData()
            remove()
            //todo checkFail
            remainingDuration = scheduledTime - env.now()
        }

        scheduledTime = Double.MAX_VALUE

        env.printTrace(now(), this, " passivate", "TODO merge_blanks(_modetxt(self._mode))")

        status = PASSIVE
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

        process = null;
        scheduledTime = Double.MAX_VALUE

        env.printTrace(now(), this, "cancel", "")

        status = DATA
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

        scheduledTime = env.now()
        env.addStandy(this)

        env.printTrace(now(), this, "standby", "todo _modetxt(self._mode)")

        status = STANDBY
    }

    /**
     * request from a resource or resources
     *
     *  Not allowed for data components or main.

     *
     *  -------
    ``yield self.request(r1)`` |n|
    --> requests 1 from r1 |n|
    ``yield self.request(r1,r2)`` |n|
    --> requests 1 from r1 and 1 from r2 |n|
    ``yield self.request(r1,(r2,2),(r3,3,100))`` |n|
    --> requests 1 from r1, 2 from r2 and 3 from r3 with priority 100 |n|
    ``yield self.request((r1,1),(r2,2))`` |n|
    --> requests 1 from r1, 2 from r2 |n|
    ``yield self.request(r1, r2, r3, oneoff=True)`` |n|
    --> requests 1 from r1, r2 or r3 |n|
     * Request has the effect that the component will check whether the requested quantity from a resource is available. It is possible to check for multiple availability of a certain quantity from several resources.
     * See https://www.salabim.org/manual/Component.html#request
     */
    fun request(resources: List<Resource>, failAt: Double? = null, oneof: Boolean = false) {

        //todo oneof_request

        if (status != CURRENT) {
            requireNotData()
            requireNotMain()
            remove()
            //todo checkFail
        }

        scheduledTime = failAt ?: Double.MAX_VALUE
        failed = false

        val q = 1

        val priority = Int.MAX_VALUE

        val r = resources.first()

        if (r.isPreemptive && resources.size > 1) {
            throw IllegalArgumentException("preemptive resources do not support multiple resource requests")
        }

//        if(requests.contains(r)){
        //  is same resource is specified several times, just add them up

        //https://stackoverflow.com/questions/53826903/increase-value-in-mutable-map
        requests.merge(r, 1, Int::plus)
//        }

        enterSorted(r.requesters, priority)

        env.printTrace(now(), this, REQUESTING.toString(), r.name!!)

        if (r.isPreemptive) {
            val av = r.availableQuantity()
            val claimers = r.claimers

            for (c in claimers.reversed()) {
                if (av >= q) {
                    break
                }

                if (priority >= claimers.first { it == r }.priority) {
                    break
                }

                TODO()
                // unclear
//                av+= c._claims.toMap().claimers.first { it ==c }
            }
        }

        tryRequest()

        if (requests.isNotEmpty()) {
            reschedule(scheduledTime, 0, false, "request")
        }
    }

    private fun tryRequest() {
        TODO("Not yet implemented")
    }

    private fun name(): String {
        return name ?: javaClass.name + "TODO Counter"

    }

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

    fun terminate() {
        claims.forEach { (resource, quantity) ->
            resource.release(quantity)
        }

        status = DATA
        scheduledTime = Double.MAX_VALUE
        process = null
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
        require(scheduledTime >= env.now()) { "scheduled time (${scheduledTime}) before now (${env.now()})" }

        this.scheduledTime = scheduledTime

        if (this.scheduledTime != Double.MAX_VALUE) push(this.scheduledTime, priority, urgent)

        //todo implement extra
        val extra = ""

        // calculate scheduling delta
        val delta = if (this.scheduledTime == env.now() || (this.scheduledTime == Double.MAX_VALUE)) "" else {
            "+" + (this.scheduledTime - env.now())
        }

        // print trace
        env.printTrace(now(), this, "$caller $delta", extra)
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

        val p: FunPointer? = if (process == null && status == DATA) {
            Component::process
        } else {
            process

            //todo
//            if inspect.isgeneratorfunction(p):
//            self._process = p(**kwargs)
//            self._process_isgenerator = True
//            else:
//            self._process = p
//            self._process_isgenerator = False
//            self._process_kwargs = kwargs
        }

        this.process = ingestFunPointer(process)


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
            at + env.offset + delay
        }

        val extra = if (p != null) "process ${p.name}" else ""
        reschedule(scheduledTime, priority, urgent, "hold", extra)

        return (this)
    }

    private fun checkFail() {
        TODO("Not yet implemented")
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
        env.eventQueue.add(QueueElement(scheduledTime, priority, seq, this))
    }

    override fun toString(): String {
        //todo implement bits from print_info
        return "todo implement bits from print_info" + super.toString()
    }

    fun callProcess() = process!!.call()
}

data class QueueElement(val time: Double, val priority: Int, val seq: Int, val component: Component) :
    Comparable<QueueElement> {
    override fun compareTo(other: QueueElement): Int {
        return compareValuesBy(this, other, { it.time}, {it.priority})
    }

    override fun toString(): String {
        return "${component.javaClass.simpleName}(${component.name}, $time, $priority, $seq)"
    }
}

enum class State {
    DATA, CURRENT, STANDBY, PASSIVE, INTERRUPTED, SCHEDULED, REQUESTING, WAITING
}
