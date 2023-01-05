@file:Suppress("ConvertSecondaryConstructorToPrimary", "unused", "MemberVisibilityCanBePrivate")

package org.kalasim.examples.taxiinc.opt1

import org.kalasim.examples.taxiinc.*
import org.optaplanner.core.api.domain.entity.PlanningEntity
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty
import org.optaplanner.core.api.domain.solution.PlanningScore
import org.optaplanner.core.api.domain.solution.PlanningSolution
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider
import org.optaplanner.core.api.domain.variable.AbstractVariableListener
import org.optaplanner.core.api.domain.variable.PlanningListVariable
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore
import org.optaplanner.core.api.score.director.ScoreDirector
import org.optaplanner.core.api.score.stream.Constraint
import org.optaplanner.core.api.score.stream.ConstraintFactory
import org.optaplanner.core.api.score.stream.ConstraintProvider
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.config.solver.EnvironmentMode
import org.optaplanner.core.config.solver.SolverConfig
import org.optaplanner.core.config.solver.termination.TerminationCompositionStyle
import org.optaplanner.core.config.solver.termination.TerminationConfig
import kotlin.properties.Delegates
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


class CleverDispatcher : FifoDispatcher() {

    var currentSchedule: TaxiSchedule? = null

    override fun bookTaxi(order: Order) {
        super.bookTaxi(order)

        //optimize schedule
        val ts = TaxiSchedule()

        val solver = configureSolver().buildSolver()
        currentSchedule = solver.solve(ts)
    }

    override fun getJob(taxi: Taxi): Job? {
        val first = currentSchedule?.jobs?.first { it.taxiId == taxi.name } ?: return null

        return Job(first.orders.map { optOrder -> orders.first { it.name == optOrder.orderId } })
    }
}

fun configureSolver(): SolverFactory<TaxiSchedule> =
    SolverFactory.create(SolverConfig()
        .withMoveThreadCount("10")
        .withSolutionClass(TaxiSchedule::class.java)
        .withEntityClasses(OptJob::class.java)
        .withConstraintProviderClass(ConstraintsProvider::class.java)
        .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
        .withTerminationConfig(TerminationConfig().apply {
                    withTerminationCompositionStyle(TerminationCompositionStyle.AND)
                    withUnimprovedSecondsSpentLimit(10)
//                    withBestScoreFeasible(true)
//                    if (config.enableStepCountTermination) {
//                        if (config.solvingStepLimit != null)
//                            withStepCountLimit(config.solvingStepLimit)
//                        if (config.scheduleUnimprovedStepLimit != null)
//                            withUnimprovedStepCountLimit(config.scheduleUnimprovedStepLimit)
//                    }
                })
        .withMoveThreadCount(SolverConfig.MOVE_THREAD_COUNT_NONE)
//        .withPhases(
//            ConstructionHeuristicPhaseConfig().apply {
//            constructionHeuristicType = ConstructionHeuristicType.FIRST_FIT
//        },
//            LocalSearchPhaseConfig().apply {
//                localSearchType = LocalSearchType.HILL_CLIMBING
//                terminationConfig = TerminationConfig().apply {
//                    withTerminationCompositionStyle(TerminationCompositionStyle.AND)
//                    withUnimprovedSecondsSpentLimit(10)
//                    withBestScoreFeasible(true)
////                    if (config.enableStepCountTermination) {
////                        if (config.solvingStepLimit != null)
////                            withStepCountLimit(config.solvingStepLimit)
////                        if (config.scheduleUnimprovedStepLimit != null)
////                            withUnimprovedStepCountLimit(config.scheduleUnimprovedStepLimit)
////                    }
//                }
//
//            }).withTerminationConfig(TerminationConfig().apply { withSecondsSpentLimit(100) })
    )

@PlanningSolution
class TaxiSchedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "orders")
    lateinit var orders: List<OptOrder>

//    @ProblemFactCollectionProperty
//    @ValueRangeProvider(id = "fleet")
//    lateinit var taxi: List<OptTaxi>


    @PlanningEntityCollectionProperty
    lateinit var jobs: List<OptJob>


    @PlanningScore
    val score: HardMediumSoftScore? = null

    /** Needed by optaplaner */
    constructor()

    constructor(orders: List<OptOrder>, taxiJobs: List<OptJob>) {
        this.orders = orders
        this.jobs = taxiJobs
    }
}

//
//class OptTaxi {
//    var id by Delegates.notNull<String>()
//
//    constructor()
//
//    constructor(id: String) {
//        this.id = id
//    }
//}


@PlanningEntity
class OptJob {

    @PlanningListVariable(valueRangeProviderRefs = ["orders"])
    lateinit var orders: List<OptOrder>
//    @PlanningVariable(valueRangeProviderRefs = ["orders"])
//    lateinit var order: OptOrder

    //    @PlanningVariable(valueRangeProviderRefs = ["fleet"])
    lateinit var taxiId: String
    var capacity by Delegates.notNull<Int>()
    var readyIn: Duration = 0.minutes

    constructor()

    constructor(orders: List<OptOrder>, taxiId: String, capacity: Int, readyIn: Duration) {
        this.orders = orders
        this.taxiId = taxiId
        this.capacity = capacity
        this.readyIn = readyIn
    }

    //    @ShadowVariable(
//        variableListenerClass = CabinCapacityListener::class, sourceVariableName = "orders"
//    )
//    val cabinCapacity: Int
}

class OptOrder {

    val from: Quarter
    val to: Quarter
    val plannedStart: Int

    var orderId by Delegates.notNull<String>()
    var passengers by Delegates.notNull<Int>()
//    lateinit var orderId: orderId

    constructor(
        orderId: String,
        from: Quarter,
        to: Quarter,
        plannedStart: Int,
        numPassengers: Int,
    ) { //        this.order = order
        this.orderId = orderId
        this.from = from
        this.to = to
        this.plannedStart = plannedStart
        this.passengers = numPassengers
//        this.passengers = passengers
    }
}

class CabinCapacityListener : AbstractVariableListener<TaxiSchedule, OptJob> {
    override fun beforeEntityAdded(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: OptJob?) {

    }

    override fun afterEntityAdded(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: OptJob?) {
        println("updating something")
    }

    override fun beforeEntityRemoved(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: OptJob?) {
    }

    override fun afterEntityRemoved(scoreDirector: ScoreDirector<TaxiSchedule>?, entity: OptJob?) {
    }
}

class ConstraintsProvider : ConstraintProvider {
    override fun defineConstraints(constraintFactory: ConstraintFactory): Array<Constraint> {
        return arrayOf(ensureCabinCapacity(constraintFactory))
    }

    private fun ensureCabinCapacity(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(OptJob::class.java)
            .filter { job -> job.orders.sumOf { it.passengers } > job.capacity }
            .penalize(
                HardMediumSoftScore.ONE_HARD,
//                ToIntFunction { it->
//                    cartesianProduct(it.orders, it.orders).map{ Quarter.distance(it.first, it.second.)}
//                }
            )
            .asConstraint("cabin-capacity")
    }

}

fun createSchedule(numOrder: Int = 10, numTaxis: Int = 3): TaxiSchedule {
    val rand = Random(42)

    val orders = List(numOrder) {
        OptOrder(
            "order$it",
            Quarter.values().random(rand),
            Quarter.values().random(rand),
            rand.nextInt(10),
            rand.nextInt(1, 5)
        )
    }

    val jobs = List(numTaxis) { OptJob(listOf(), "taxi$it", 4, rand.nextInt(15).minutes) }

    return TaxiSchedule(orders, jobs)
}

fun main() {
    val solver = configureSolver().buildSolver()
    val solved = solver.solve(createSchedule())

    println(solved)


    println(GSON.toJson(solved))
}
