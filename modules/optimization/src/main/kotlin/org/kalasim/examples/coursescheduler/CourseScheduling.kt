package com.systema.peo.bilbo.analysis.scratch

import ai.timefold.solver.core.api.domain.entity.PlanningEntity
import ai.timefold.solver.core.api.domain.lookup.PlanningId
import ai.timefold.solver.core.api.domain.solution.*
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider
import ai.timefold.solver.core.api.domain.variable.*
import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore
import ai.timefold.solver.core.api.score.director.ScoreDirector
import ai.timefold.solver.core.api.score.stream.*
import ai.timefold.solver.core.api.score.stream.Joiners.overlapping
import ai.timefold.solver.core.api.solver.SolutionManager
import ai.timefold.solver.core.api.solver.SolverFactory
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig
import ai.timefold.solver.core.config.solver.SolverConfig
import ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle
import ai.timefold.solver.core.config.solver.termination.TerminationConfig
import kotlinx.datetime.*
import org.jetbrains.kotlinx.dataframe.api.print
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.math.sqrt
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit


// settings
const val hourlyCoffeeMachineOutput = 2.5
const val coffeePerPersonAndHoursLiters = 0.2


@PlanningEntity
abstract class ScheduleItem {

    @get:PlanningId
    var id: String? = null

    @InverseRelationShadowVariable(sourceVariableName = "previousTaskOrRoom")
    var nextTask: Course? = null

    abstract val end: Instant?
}

class Room() : ScheduleItem() {

    constructor(name: String, availableFrom: Instant) : this() {
        this.id = name
        this.end = availableFrom
    }

    override fun toString() = id!!

    override lateinit var end: Instant


    val roomSchedule: List<Course>
        get() = buildList {
            var nextTask = nextTask

            while(nextTask != null) {
                add(nextTask)
                nextTask = nextTask.nextTask
            }
        }
}


@PlanningEntity
class Course() : ScheduleItem() {

    constructor(title: String, numParticipants: Int, duration: Duration) : this() {
        id = title

        this.numParticipants = numParticipants
        this.duration = duration
    }

    var numParticipants: Int = 0
    var duration by Delegates.notNull<Duration>()


    @AnchorShadowVariable(sourceVariableName = "previousTaskOrRoom")
    var room: Room? = null

    @PlanningVariable(
        valueRangeProviderRefs = ["rooms"],
        graphType = PlanningVariableGraphType.CHAINED
    )
    var previousTaskOrRoom: ScheduleItem? = null

    @ShadowVariable(
        variableListenerClass = CourseVariablesListener::class,
        sourceVariableName = "room"
    )
    @ShadowVariable(
        variableListenerClass = CourseVariablesListener::class,
        sourceVariableName = "previousTaskOrRoom"
    )
    var start: Instant? = null

    @PiggybackShadowVariable(shadowVariableName = "start")
    override var end: Instant? = null

    override fun toString() = "$id::$room($start-$end)"

    /**
     * Calculates the overlap between the given date and the schedule item.
     */
    fun calculateOverlap(date: LocalDate): Duration {
        val dayStart = date.asInstant()
        val dayEnd = date.asInstant() + 1.days

        if(start!! >= dayEnd || dayStart >= end!!) return Duration.ZERO

        val overlapStart = maxOf(start!!, dayStart)
        val overlapEnd = minOf(end!!, dayEnd)
        return overlapEnd - overlapStart
    }

    fun coffeeDemandAt(date: LocalDate): Double {
        // note for sake of simplicity, we do not consider that the
        // workshop will actually take place during business hours (8-16)
        val courseHoursAtDate = calculateOverlap(date).toDouble(DurationUnit.HOURS)
        return courseHoursAtDate * numParticipants * coffeePerPersonAndHoursLiters
    }
}

fun createCourseSolveConfig(): SolverConfig =
    SolverConfig()
        .withSolutionClass(CourseSchedule::class.java)
        .withEntityClasses(Course::class.java, ScheduleItem::class.java)
        .withConstraintProviderClass(CourseConstraints::class.java)

        .withPhases(
            ConstructionHeuristicPhaseConfig()
                .apply {
//                constructionHeuristicType = ConstructionHeuristicType.FIRST_FIT
                },
            LocalSearchPhaseConfig().apply {
                terminationConfig = TerminationConfig().apply {
                    withStepCountLimit(30)
                    withTerminationCompositionStyle(TerminationCompositionStyle.OR)
                    withUnimprovedStepCountLimit(10)
                }
            }
        )


@PlanningSolution
class CourseSchedule {
    constructor(courses: List<Course>, rooms: List<Room>, scheduleStart: LocalDate, maxDays: Int) {
        this.courses = courses
        this.rooms = rooms

        coffeeDemandTimeline = List(maxDays) { scheduleStart.plus(it, DateTimeUnit.DAY) }
    }

    @ProblemFactCollectionProperty
    lateinit var coffeeDemandTimeline: List<LocalDate>

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "rooms")
    lateinit var rooms: List<Room>

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "courses")
    lateinit var courses: List<Course>

    @PlanningScore
    var score: HardMediumSoftScore? = null

    @Suppress("ConvertSecondaryConstructorToPrimary", "unused")
    constructor() // needed by solver engine
}


class CourseVariablesListener : BasicVariableListener<CourseSchedule, Course>() {

    override fun afterEntityAdded(scoreDirector: ScoreDirector<CourseSchedule>, entity: Course) {
        afterVariableChanged(scoreDirector, entity)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun afterVariableChanged(scoreDirector: ScoreDirector<CourseSchedule>, rootCourse: Course) {
        var course: Course? = rootCourse

        while(course != null) {
            scoreDirector.change(Course::start.name, course) {
                start = previousTaskOrRoom?.end
            }

            scoreDirector.change(Course::end.name, course) {
                end = start?.let { it + duration }
            }

            course = course.nextTask
        }
    }
}

open class CourseConstraints : ConstraintProvider {

    override fun defineConstraints(constraintFactory: ConstraintFactory): Array<Constraint> = buildList {
        add(minimizeMakespan(constraintFactory))
        add(coffeeAvailability(constraintFactory))
    }.toTypedArray()

    // makespan minimization
    // can't work becaue no incentive as sum over all rooms does never change
    fun minimizeMakespan2(constraintFactory: ConstraintFactory) = constraintFactory
        .forEach(Room::class.java)
        .penalize(HardMediumSoftScore.ONE_MEDIUM) { room ->
            val hoursSinceStart = (room.roomSchedule.lastOrNull()?.end ?: room.end).minus(room.end).inWholeHours
            sqrt(hoursSinceStart.toDouble()).toInt()
        }
        .asConstraint("minimize-makespan")


    fun minimizeMakespan(constraintFactory: ConstraintFactory) = constraintFactory
        .forEach(Course::class.java)
        .filter { it.end != null } // course is schedule
        .penalize(HardMediumSoftScore.ONE_MEDIUM) { course ->
            (course.end!! - course.room?.end!!).inWholeDays.toInt()
        }
        .asConstraint("minimize-makespan")

    // coffee
    fun coffeeAvailability(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory
            .forEach(LocalDate::class.java)
            .join(
                Course::class.java, // must be planning fact or planning entity
                overlapping(
                    LocalDate::startOfDay, // we can use method references or lambdas here
                    LocalDate::endOfDay,
                    { course: Course -> course.start!! },
                    { course: Course -> course.end!! })
            )
            .groupBy({ day, course -> day }, { date: LocalDate, course: Course ->
                // compute proportional coffee demand
                course.coffeeDemandAt(date).apply {
//                    println(this)
                }
            })
            .filter { _, consumedCoffee ->
                consumedCoffee > hourlyCoffeeMachineOutput * 24
            }
            .penalize(HardMediumSoftScore.ONE_SOFT)
            .asConstraint("lack-of-coffee")
    }
}

val LocalDate.startOfDay
    get() = atStartOfDayIn(TimeZone.UTC)
val LocalDate.endOfDay
    get() = plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC)


fun main() {
    val scheduleStart = LocalDate(2024, 1, 1).asInstant()
    val rooms = listOf(
        Room("Room A", scheduleStart),
        Room("Room B", scheduleStart),
    )

//    val partDist = UniformIntegerDistribution(JDKRandomGenerator(42), 10, 10)
//    val durationDist = UniformIntegerDistribution(JDKRandomGenerator(42), 10, 10)
    val courses = listOf(
        Course("C1", 9, 3.days),
        Course("C2", 13, 36.hours),
        Course("C3", 8, 2.days),
        Course("C4", 7, 4.days),
        Course("C5", 15, 4.days),
    )

    val unsolved = CourseSchedule(courses, rooms, LocalDate(2024, 1, 1), 20)

    val solverFactory = SolverFactory.create<CourseSchedule>(createCourseSolveConfig())
    val solved = solverFactory.buildSolver().solve(unsolved)

    solved.printSchedule()
    solved.analyzeSchedule(solverFactory)
}

private fun CourseSchedule.analyzeSchedule(solverFactory: SolverFactory<CourseSchedule>) {
    val explain = SolutionManager.create(solverFactory).explain(this)
    explain.justificationList.forEach {
        val message = it as DefaultConstraintJustification
        println("""${message.facts.joinToString(",")} ${message.getImpact()}""")
    }
    explain.constraintMatchTotalMap.values.toDataFrame {
        "constraint" from { it.constraintRef.constraintName }
        "matchCount" from { it.constraintMatchCount }
        "score" from { it.score }
    }.print()
}

fun CourseSchedule.printSchedule() {
    println("Course Schedule (numTools = ${rooms.size}, numCourses = ${courses.size})")
    for(room in rooms) {
        print("${room.id}: ")
        val callSeq = room.roomSchedule.joinToString(" -> ") { it.toString() }
        println(callSeq)
    }

    println("Unassigned tasks are: ${courses.filter { it.room == null }}")
}

