@file:Suppress("ConvertSecondaryConstructorToPrimary", "unused", "MemberVisibilityCanBePrivate")

package org.kalasim.examples.taxiinc.opt2

import org.jetbrains.kotlinx.dataframe.math.mean
import org.jetbrains.kotlinx.dataframe.math.median
import org.kalasim.TickTime
import org.kalasim.examples.taxiinc.*
import org.optaplanner.core.api.domain.entity.PlanningEntity
import org.optaplanner.core.api.domain.lookup.PlanningId
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty
import org.optaplanner.core.api.domain.solution.PlanningScore
import org.optaplanner.core.api.domain.solution.PlanningSolution
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider
import org.optaplanner.core.api.domain.variable.AbstractVariableListener
import org.optaplanner.core.api.domain.variable.PlanningVariable
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore
import org.optaplanner.core.api.score.director.ScoreDirector
import org.optaplanner.core.api.score.stream.Constraint
import org.optaplanner.core.api.score.stream.ConstraintCollectors.*
import org.optaplanner.core.api.score.stream.ConstraintFactory
import org.optaplanner.core.api.score.stream.ConstraintProvider
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.config.solver.EnvironmentMode
import org.optaplanner.core.config.solver.SolverConfig
import org.optaplanner.core.config.solver.termination.TerminationConfig
import kotlin.math.absoluteValue
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


class CleverDispatcher : FifoDispatcher() {

    var currentSchedule: Map<org.kalasim.examples.taxiinc.Taxi, Job> = mapOf()

    var bookings = 0

    override fun bookTaxi(order: org.kalasim.examples.taxiinc.Order) {
        super.bookTaxi(order)

        bookings++

//        if(bookings.rem(10) != 0) return
//
//        updateSchedule()

        // schedule veicles if needed
        currentSchedule.forEach { taxi, job ->
//            job.plannedStart-15.minutes
            if(taxi.isPassive) taxi.activate()
        }
    }

    private fun updateSchedule() {
        if(orders.isEmpty()) {
            currentSchedule = mapOf()
            return
        }
        //optimize schedule
        val ts = TaxiSchedule().apply {
            orders = this@CleverDispatcher.orders.map {
                Order().apply {
                    orderId = it.name
                    from = it.from
                    to = it.to
                    passengers = it.numPassengers
                    plannedStart = (now - it.plannedStart).toDuration()
                }
            }

            fleet = this@CleverDispatcher.fleet.map {
                Taxi().apply {
                    taxiId = it.name
                    capacity = it.cabinCapacity.capacity.toInt()
                    readyIn = (now - (it.estArrivalTime ?: now)).toDuration()
//                        if(it.status.value == TaxiStatus.Driving) ()
                    readyPosition = it.finalJobDestination ?: it.position
                }
            }
        }

        val solver = configureSolver().buildSolver()
        val optimizedSchedule = solver.solve(ts)

        // convert into sim-data-model

        this.currentSchedule = optimizedSchedule!!.orders.filter { it.taxi != null }
            .groupBy { it.taxi }
            .map { (schdTaxi, schdOrders) ->
                fleet.find { it.name == schdTaxi!!.taxiId }!! to Job(schdOrders.map { optOrder ->
                    orders.first { it.name == optOrder.orderId }
                })
            }
            .filter { it.second.orders.isNotEmpty() }
            .toMap()
    }

    override fun getJob(taxi: org.kalasim.examples.taxiinc.Taxi): Job? {
        if(orders.isEmpty()) return null

        // workload but taxi not scheduled?
        if(orders.isNotEmpty() && currentSchedule[taxi] == null) {
            updateSchedule()
        }

        val nextJob = currentSchedule[taxi]


        // consume the job
        currentSchedule = currentSchedule.minus(taxi)

        if(nextJob != null) orders.removeAll(nextJob.orders)


        return nextJob
    }
}

fun configureSolver(): SolverFactory<TaxiSchedule> = SolverFactory.create(
    SolverConfig()
        .withSolutionClass(TaxiSchedule::class.java)
        .withEntityClasses(Order::class.java)
        .withConstraintProviderClass(ConstraintsProvider::class.java)
        .withEnvironmentMode(EnvironmentMode.FAST_ASSERT)
        .withTerminationConfig(TerminationConfig().apply {
//            withSecondsSpentLimit(10)
//            withBestScoreFeasible(true)
            withUnimprovedSecondsSpentLimit(10)
//    withStepCountLimit(config.solvingStepLimit)
//    withUnimprovedStepCountLimit(config.scheduleUnimprovedStepLimit)
        })
        .withMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_AUTO)
)

@PlanningSolution
class TaxiSchedule {

    @PlanningEntityCollectionProperty
    lateinit var orders: List<Order>

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "fleet")
    lateinit var fleet: List<Taxi>

    @PlanningScore
    val score: HardMediumSoftScore? = null
}


class Taxi {
    @PlanningId
    lateinit var taxiId: String
    var capacity by Delegates.notNull<Int>()

    var readyIn by Delegates.notNull<Duration>()

    lateinit var readyPosition: Quarter
}

@PlanningEntity
class Order {
    var estArrivalTime: TickTime? = null
    var finalJobDestination: Quarter? = null

    lateinit var from: Quarter
    lateinit var to: Quarter
    var plannedStart by Delegates.notNull<Duration>()

    @PlanningId
    lateinit var orderId: String

    var passengers by Delegates.notNull<Int>()

    @PlanningVariable(nullable = true, valueRangeProviderRefs = ["fleet"])
    var taxi: Taxi? = null

//    fun isTaxiInitialized() = ::taxi.isInitialized

}

class CabinCapacityListener : AbstractVariableListener<TaxiSchedule, Taxi> {
    override fun beforeEntityAdded(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: Taxi?) = Unit

    override fun afterEntityAdded(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: Taxi?) = Unit

    override fun beforeEntityRemoved(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: Taxi?) = Unit

    override fun afterEntityRemoved(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: Taxi?) = Unit
}

/** Standard deviation.*/
private fun List<Double>.sd(): Double {
    val mean = mean()
    return Math.sqrt(sumOf { Math.pow(it - mean, 2.0) })
}

class ConstraintsProvider : ConstraintProvider {
    override fun defineConstraints(constraintFactory: ConstraintFactory) = arrayOf(
        ensureCabinCapacity(constraintFactory),
        respectReadyTime(constraintFactory),
        maximizeAssignedOrders(constraintFactory),
//        minimizeNextJobStart(constraintFactory),
        sameStart(constraintFactory),
        sameDestination(constraintFactory),
//        maximeOntimePickup(constraintFactory),
        //        preferSimilarRoutes(constraintFactory),
        //        bundleNearbyOrders(constraintFactory),
    )


    private fun ensureCabinCapacity(constraintFactory: ConstraintFactory) = constraintFactory.forEach(Order::class.java)
        .filter { it.taxi != null }
        .groupBy(Order::taxi, sum(Order::passengers))
        .filter { taxi, passengers -> passengers > taxi!!.capacity }
        .penalize(
            HardMediumSoftScore.ONE_HARD,
//                ToIntFunction { it->
//                    cartesianProduct(it.orders, it.orders).map{ Quarter.distance(it.first, it.second.)}
//                }
        )
        .asConstraint("ensure-cabin-capacity")


    private fun respectReadyTime(constraintFactory: ConstraintFactory) = constraintFactory.forEach(Order::class.java)
        .filter { it.taxi != null }
        .filter { order -> order.plannedStart < order.taxi!!.readyIn }
        .penalize(HardMediumSoftScore.ONE_HARD)
        .asConstraint("respect-ready-time")

    private fun maximizeAssignedOrders(constraintFactory: ConstraintFactory) =
        constraintFactory.forEach(Order::class.java)
            .filter { it.taxi == null }
            .reward(HardMediumSoftScore.ONE_MEDIUM)
            .asConstraint("minimize-unassigned")

    private fun sameStart(constraintFactory: ConstraintFactory): Constraint =
        constraintFactory.forEach(Order::class.java)
            .groupBy(Order::taxi, toSet())
            .reward(HardMediumSoftScore.ONE_SOFT) { taxi, orders ->
                orders.groupingBy { it.from }
                    .eachCount()
                    .maxBy { it.value }.value
            }
            .asConstraint("same-start")

    private fun sameDestination(constraintFactory: ConstraintFactory): Constraint =
        constraintFactory.forEach(Order::class.java)
            .groupBy(Order::taxi, toSet())
//            .filter { taxi, orders -> orders.distinctBy { it.to }.size == 1 }
//            .reward(HardMediumSoftScore.ONE_MEDIUM)
            .reward(HardMediumSoftScore.ONE_SOFT) { taxi, orders ->
                orders.groupingBy { it.to }
                    .eachCount()
                    .maxBy { it.value }.value
            }
            .asConstraint("same-dest")

    private fun minimizeTotalDriveDistance(constraintFactory: ConstraintFactory) =
        constraintFactory.forEach(Order::class.java)
            .groupBy(Order::taxi, toSet())
            .penalize(HardMediumSoftScore.ONE_SOFT){ taxi, orders ->
                // note: there is no inherent order in the route so we can not compute the
                //       driven route length --> need to use VRP
                1

            }
            .asConstraint("minimize-distance")


//    private fun preferSimilarRoutes(constraintFactory: ConstraintFactory): Constraint =
//        constraintFactory.forEach(Order::class.java)
//            .groupBy(Order::taxi, toList())
//            .penalize(HardMediumSoftScore.ONE_MEDIUM) { taxi, orders ->
//                val fromCoord = orders.map { it.from.coordinates }
//                fromCoord.map { it.x.toDouble() }.sd() + fromCoord.map { it.y.toDouble() }.sd()
//            }.asConstraint("bundle-nearby")

    private fun bundleNearbyOrders(constraintFactory: ConstraintFactory): Constraint =
        constraintFactory.forEach(Order::class.java)
            .groupBy(Order::taxi, toList())
            .penalize(HardMediumSoftScore.ONE_SOFT) { taxi, orders ->
                val coord = orders.map { it.from.coordinates }
                val meanX = coord.map { it.x }
                    .median()
                val meanY = coord.map { it.y }
                    .median()

                coord.map { (it.x - meanX).absoluteValue + (it.y - meanY).absoluteValue }
                    .mean()
                    .toInt()
            }
            .asConstraint("bundle-nearby")


    private fun minimizeNextJobStart(constraintFactory: ConstraintFactory) =
        constraintFactory.forEach(Order::class.java)
            .filter { it.taxi != null }
            .groupBy(Order::taxi, toList())
            .penalize(HardMediumSoftScore.ONE_SOFT) { taxi, orders ->
                orders.minOf { Quarter.distance(taxi!!.readyPosition, it.from) }
            }
            .asConstraint("min-pickup-distance")

//    private fun maximeOntimePickup(constraintFactory: ConstraintFactory) =
//        constraintFactory.forEach(Order::class.java)
//            .filter { it.plannedStart < 10.minutes }.reward(HardMediumSoftScore.ONE_SOFT)
//            .asConstraint("maximize-ontime-pickup")
}

fun createSchedule(numOrder: Int = 100, numTaxis: Int = 10): TaxiSchedule {
    val rand = Random(42)

    val orders = List(numOrder) {
        Order().apply {
            orderId = "order$it"
            from = Quarter.values()
                .random(rand)
            to = Quarter.values()
                .random(rand)
            plannedStart = rand.nextInt(10).minutes
            passengers = rand.nextInt(1, 5)
        }
    }

    val fleet = List(numTaxis) {
        Taxi().apply {
            taxiId = "taxi$it"
            capacity = 4
            readyIn = rand.nextInt(15).minutes
            readyPosition = Quarter.values()
                .random(rand)
        }
    }

    return TaxiSchedule().apply {
        this.orders = orders
        this.fleet = fleet
    }
}

fun main() {
    val solver = configureSolver().buildSolver()
    val solved = solver.solve(createSchedule(numOrder = 1))

    println(solved)

    println("serializing schedule")
    println(GSON.toJson(solved))

    println(solved.orders.filter { it.taxi != null }
        .groupBy { it.taxi }
        .map { (taxi, orders) -> taxi!!.taxiId to orders.joinToString { it.orderId } })
}
