@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.examples.taxiinc;

import com.github.holgerbrandl.jsonbuilder.json
import org.kalasim.examples.taxiinc.TaxiStatus.*
import org.jetbrains.kotlinx.dataframe.math.mean
import org.kalasim.*
import org.kalasim.misc.mergeStats
import java.awt.Point
import kotlin.math.absoluteValue
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

class Taxi : Component() {
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
            // collect all passengers
            job.passengers.forEach { request ->
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
            job.passengers.forEach { request ->
                if(request.to != position) {
                    driveTo(request.from)
                    release(cabinCapacity, quantity = request.numPassengers)
                    log(OrderChangeEvent(request, OrderStatus.Completed, now))
                }
            }

            dispatcher.jobCompleted(job)
        }
    }


    private suspend fun SequenceScope<Component>.driveTo(destination: Quarter) {
        val distanceKm = Quarter.distance(position, destination).toDouble()

        val speedKmh = 40.0 // meters per minute
        status.value = Driving
        hold((distanceKm / speedKmh).hours)

        log(TaxiDriveEvent(cabinCapacity.claimed.toInt(), distanceKm, now))
    }
}


class TaxiDriveEvent(val cabinLoad: Int, val distance: Double, ts: TickTime) : Event(ts)


data class Order(
    val from: Quarter,
    val to: Quarter,
    val plannedStart: TickTime,
    val numPassengers: Int = 1,
) : Component() {
    override fun process() = sequence {
        hold(until = plannedStart)
        log(OrderChangeEvent(this@Order, OrderStatus.Waiting, now))
    }

    val tripDistance = Quarter.distance(from, to)
}

enum class OrderStatus { Created, Dispatched, Waiting, Pickup, Completed }

class OrderChangeEvent(val order: Order, val status: OrderStatus, timestamp: TickTime) : Event(timestamp)


data class Job(val passengers: List<Order>)

interface Dispatcher {
    val orders: ComponentList<Order>
    fun bookTaxi(order: Order)
    fun getJob(taxi: Taxi): Job?
    fun jobCompleted(job: Job)
}

open class FifoDispatcher(val fleet: List<Taxi>) : Component(), Dispatcher {

    override fun process() = sequence {
        val nextOrder = orders.minByOrNull { it.plannedStart }

        if(nextOrder != null) {
            val timeUntilPickup = (now - nextOrder.plannedStart).toDuration()

            val prePickup = 10.minutes

            if(timeUntilPickup <= prePickup) {
                fleet.firstOrNull { it.isPassive }?.activate()
            } else {
                hold(timeUntilPickup - prePickup)
            }
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

    override fun jobCompleted(job: Job) {
//        println("job_completed")
    }
}

fun main() {
    createSimulation {
        val taxis = List(10) { Taxi() }

        val dispatcher = dependency<Dispatcher> { FifoDispatcher(taxis) }

        val delay = uniform(0, 60).minutes

        ComponentGenerator(uniform(0, 10)) {
            val departure = Quarter.values().random()
            val destination = Quarter.values().asList().minusElement(departure).random()

            Order(departure, destination, env.now + delay()).apply { dispatcher.bookTaxi(this) }
        }

        val drives = collect<TaxiDriveEvent>()
        val orderTx = collect<OrderChangeEvent>()

        run(10.days)

        // compute central metrics: avg idle time & avg profit
        val idleProportion = taxis.map { it.status.timeline }.mergeStats()[Idle]

        val costPerKm = 1.3
        val pricePerKm = 3
        val dailyProfit = drives.map { it.distance * (it.cabinLoad * pricePerKm - costPerKm) }.sum() / 10


        val avgDispatchQueueLength = dispatcher.orders.sizeTimeline.statistics().mean
        dispatcher.orders.sizeTimeline.statistics().printJson()
//        dispatcher.orders.sizeTimeline.printHistogram()

        val avgDistance = orderTx.map { it.order }.toSet().map { it.tripDistance }.mean()

        val avgDriveTime = orderTx
        .filter { listOf(OrderStatus.Pickup, OrderStatus.Completed).contains(it.status) }
        .groupBy { it.order }
        .filter { it.value.size == 2 }
        .map { (_, tx) -> (tx.last().time - tx.first().time) }.mean().toDuration()



        val avgWaitTime4Pickup = orderTx
            .filter { listOf(OrderStatus.Waiting, OrderStatus.Pickup).contains(it.status) }
            .groupBy { it.order }
            .filter { it.value.size == 2 }
            .map { (_, tx) -> (tx.last().time - tx.first().time) }.mean().toDuration()

        val metrics = json {
            "idleprop" to idleProportion
            "dailyProfit" to dailyProfit
            "numJobs" to orderTx.distinctBy { it.order }.count()
            "avgQueueLength" to avgDispatchQueueLength
            "avgWaitTime4Pickup" to avgWaitTime4Pickup
            "averageDistance" to avgDistance
            "avgDriveTime" to avgDriveTime
        }

        println("sim completed: $metrics")
    }
}


