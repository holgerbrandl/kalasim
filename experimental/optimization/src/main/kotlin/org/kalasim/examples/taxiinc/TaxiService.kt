@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.examples.taxiinc;

import com.github.holgerbrandl.jsonbuilder.json
import org.jetbrains.kotlinx.dataframe.math.mean
import org.kalasim.*
import org.kalasim.examples.taxiinc.TaxiStatus.*
import org.kalasim.examples.taxiinc.opt2.CleverDispatcher
import org.kalasim.misc.mergeStats
import org.kalasim.misc.time.mean
import java.awt.Point
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


@Suppress("SpellCheckingInspection")
enum class Quarter {
    Altstadt, Blasewitz, Cotta, Klotzsche, Leuben, Loschwitz, Neustadt, Pieschen, Plauen, Prohlis;

    val coordinates = Point(hashCode().absoluteValue.rem(10), hashCode().absoluteValue.rem(12))

    companion object {
        // compute a deterministic non-zero distance between 2 quarters
        fun distance(quarter: Quarter, other: Quarter): Int {
            // manhattan distance
            return (quarter.coordinates.x - other.coordinates.x).absoluteValue + (quarter.coordinates.y - other.coordinates.y).absoluteValue
        }
    }
}

enum class TaxiStatus { Idle, Driving, WaitingForPickup }

class Taxi(val speedKmh: Double = 40.0) : Component() {
    val status = State(Idle)

    val cabinCapacity = Resource(capacity = 4)

    private val _position = State(Quarter.values().random())

    var position: Quarter
        get() = _position.value
        set(value) {
            _position.value = value
        }

    override fun repeatedProcess() = sequence {
        val dispatcher = get<Dispatcher>()

        val job = dispatcher.getJob(this@Taxi)

        if(job == null) {
            status.value = Idle
            passivate()
        } else {
            // predict job completion
            val totalRoute =
                (listOf(job.orders.last().from) + job.orders.map { it.to }).zipWithNext().sumOf { (from, to) ->
                    Quarter.distance(from, to)
                }
            estArrivalTime = listOf(job.orders.last().plannedStart, now).max() + computeDriveDuration(totalRoute.toDouble())
            finalJobDestination = job.orders.last().to

            // collect all passengers
            job.orders.forEach { request ->
                if(request.from != position) {
                    driveTo(request.from)
                }

                // optionally wait for passengers if too earl
                if(request.plannedStart > now) {
                    status.value = WaitingForPickup
                    hold(until = request.plannedStart)
                }

                log(OrderChangeEvent(request, OrderStatus.Pickup, now))
                request(cabinCapacity, quantity = request.numPassengers)
            }


            // drive them to their locations
            job.orders.forEach { request ->
                if(request.to != position) {
                    driveTo(request.from)
                    release(cabinCapacity, quantity = request.numPassengers)
                    log(OrderChangeEvent(request, OrderStatus.Completed, now))
                }
            }

//            finalJobDestination = null
            estArrivalTime = null
        }


    }

    var estArrivalTime: SimTime? = null
    var finalJobDestination: Quarter? = null

    private suspend fun SequenceScope<Component>.driveTo(destination: Quarter) {
        val distanceKm = Quarter.distance(position, destination).toDouble()

        val hours = computeDriveDuration(distanceKm)

        status.value = Driving
        hold(hours)
        position = destination

        log(TaxiDriveEvent(cabinCapacity.claimed.toInt(), distanceKm, now))
    }

    private fun computeDriveDuration(distanceKm: Double): Duration = (distanceKm / speedKmh).hours
}


class TaxiDriveEvent(val cabinLoad: Int, val distance: Double, ts: SimTime) : Event(ts)


data class Order(
    val from: Quarter,
    val to: Quarter,
    val plannedStart: SimTime,
    val numPassengers: Int = 1,
) : Component() {
    override fun process() = sequence {
        hold(until = plannedStart)
        log(OrderChangeEvent(this@Order, OrderStatus.Waiting, now))
    }

    val tripDistance = Quarter.distance(from, to)
}

enum class OrderStatus { Created, Dispatched, Waiting, Pickup, Completed }

class OrderChangeEvent(val order: Order, val status: OrderStatus, timestamp: SimTime) : Event(timestamp)


data class Job(val orders: List<Order>) {
    val plannedStart = orders.minOf { it.plannedStart }
}

interface Dispatcher {
    val orders: ComponentList<Order>
    fun bookTaxi(order: Order)
    fun getJob(taxi: Taxi): Job?
}

open class FifoDispatcher : Component(), Dispatcher {

    val fleet = get<List<Taxi>>()

    override fun process() = sequence {
        val next = orders.minByOrNull { it.plannedStart }
        val nextJob = next?.let { Job(listOf(it)) }

        if(nextJob != null) {
            val timeUntilPickup = now - nextJob.plannedStart

            val prePickup = 10.minutes

            if(timeUntilPickup >= prePickup) {
                hold(timeUntilPickup - prePickup)
            }

            fleet.firstOrNull { it.isPassive }?.activate()
        }
    }

    override val orders = ComponentList<Order>()

    override fun bookTaxi(order: Order) {
        orders.add(order)
        log(OrderChangeEvent(order, OrderStatus.Created, now))

        activate()
    }

    override fun getJob(taxi: Taxi): Job? {
        return if(orders.isNotEmpty()) {
            val order = orders.removeFirst()
            log(OrderChangeEvent(order, OrderStatus.Dispatched, now))
            Job(listOf(order))
        } else null
    }
}

class TaxiInc(dispatcherClass: Class<out Dispatcher>) : Environment() {
    val taxis = dependency { List(10) { Taxi() } }

    //    val dispatcher = dependency<Dispatcher> { FifoDispatcher(taxis) }
    val dispatcher = dependency<Dispatcher> { dispatcherClass.getDeclaredConstructor().newInstance() }

    val delay = uniform(0, 60).minutes

    init {
        ComponentGenerator(uniform(0, 10)) {
            val departure = Quarter.values().random()
            val destination = Quarter.values().asList().minusElement(departure).random()

            Order(departure, destination, env.now + delay()).apply { dispatcher.bookTaxi(this) }
        }
    }

    fun computeMetrics(duration: Duration = 10.days): Any {

        val drives = collect<TaxiDriveEvent>()
        val orderTx = collect<OrderChangeEvent>()

        run(duration)

        // compute central metrics: avg idle time & avg profit
        val idleProportion = taxis.map { it.status.timeline }.mergeStats()[Idle]

        val costPerKm = 1.3
        val pricePerKm = 3
        val dailyProfit = drives.map { it.distance * (it.cabinLoad * pricePerKm - costPerKm) }.sum() / 10


        val avgDispatchQueueLength = dispatcher.orders.sizeTimeline.statistics().mean
        dispatcher.orders.sizeTimeline.statistics().printJson()
//        dispatcher.orders.sizeTimeline.printHistogram()

        val avgDistance = orderTx.map { it.order }.toSet().map { it.tripDistance }.mean()

        val avgDriveTime = orderTx.filter { listOf(OrderStatus.Pickup, OrderStatus.Completed).contains(it.status) }
            .groupBy { it.order }.filter { it.value.size == 2 }.map { (_, tx) -> (tx.last().time - tx.first().time) }
            .mean()


        val avgWaitTime4Pickup =
            orderTx.filter { listOf(OrderStatus.Waiting, OrderStatus.Pickup).contains(it.status) }.groupBy { it.order }
                .filter { it.value.size == 2 }.map { (_, tx) -> (tx.last().time - tx.first().time) }.mean()

        return json {
            "idleprop" to idleProportion
            "dailyProfit" to dailyProfit
            "numJobs" to orderTx.distinctBy { it.order }.count()
            "avgQueueLength" to avgDispatchQueueLength
            "avgWaitTime4Pickup" to avgWaitTime4Pickup
            "averageDistance" to avgDistance
            "avgDriveTime" to avgDriveTime
        }

//        return object {
//            val idleprop = idleProportion
//            val dailyProfit = dailyProfit
//            val numJobs = orderTx.distinctBy { it.order }.count()
//            val avgQueueLength = avgDispatchQueueLength
//            val avgWaitTime4Pickup = avgWaitTime4Pickup
//            val averageDistance = avgDistance
//            val avgDriveTime = avgDriveTime
//        }
    }

}

fun main() {
//    val computeMetrics = TaxiInc(FifoDispatcher::class.java).computeMetrics()
    val computeMetrics = TaxiInc(CleverDispatcher::class.java).computeMetrics()

    val newInstance = FifoDispatcher::class.java.getDeclaredConstructor().newInstance()
    println(computeMetrics)
}


