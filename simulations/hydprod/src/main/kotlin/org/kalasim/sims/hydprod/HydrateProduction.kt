@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.sims.hydprod

import org.kalasim.*
import org.kalasim.sims.hydprod.HarvesterState.*
import java.awt.geom.Point2D
import java.lang.Math.pow
import java.util.concurrent.TimeUnit
import kotlin.math.*
import kotlin.properties.Delegates

val GridPosition.mapCoordinates: Point2D
    get() = Point2D.Double(x * 10.0, y * 10.0)

enum class HarvesterState { STANDBY, MOVING, SCANNING, MINING, UNLOADING, MAINTENANCE, BROKEN }

class Deposit(val gridPosition: GridPosition, size: Int) : DepletableResource(capacity = size)

data class GridPosition(val x: Int, val y: Int)

fun GridPosition.distance(other: GridPosition) =
    sqrt(pow((x - other.x).toDouble(), 2.0) - pow((y - other.y).toDouble(), 2.0))

class DepositMap(
    numDeposits: Int = 10,
    maxCapacity: Int = 1000,
    val lowerLeft: GridPosition = GridPosition(0, 0),
    val upperRight: GridPosition = GridPosition(100, 100)
) : SimulationEntity() {
    init {
        require(maxCapacity > 10)
        require(lowerLeft.x < upperRight.x)
        require(lowerLeft.y < upperRight.y)
    }

    // define probability distributions
    val capacityDist = uniform(1, maxCapacity)
    val x = discreteUniform(lowerLeft.x, upperRight.x)
    val y = discreteUniform(lowerLeft.y, upperRight.y)

    val deposits = List(numDeposits) {
        Deposit(GridPosition(x(), y()), capacityDist.sample().roundToInt())
    }.groupBy { it.gridPosition }.map { it.value.first() } // remove position duplicates

    fun isOnMap(gp: GridPosition): Boolean = TODO()

    fun restrictToMap(gp: GridPosition): GridPosition = GridPosition(
        min(max(gp.x, lowerLeft.x), upperRight.x),
        min(max(gp.y, lowerLeft.y), upperRight.y)
    )
}


open class MovingComponent(initialPosition: Point2D, name: String?=null) : Component(name) {

    private var from: Point2D = initialPosition
    var to: Point2D? = null

    private var started by Delegates.notNull<TickTime>()
    private var currentSpeed by Delegates.notNull<Double>()
    private lateinit var estimatedArrival: TickTime

    suspend fun SequenceScope<Component>.move(
        nextTarget: Point2D,
        speed: Double,
        description: String? = null,
        priority: Priority = Priority.NORMAL,
        urgent: Boolean = false
    ) {
        to = nextTarget
        started = now
        currentSpeed = speed

        val distance = distance()
        val duration = distance / speed
        estimatedArrival = now + duration

        hold(Ticks(duration), description, priority)
        to = null
    }

    private fun distance(): Double {
        val xDist = to!!.x - from.x
        val yDist = to!!.y - from.y

        return sqrt(xDist * xDist + yDist * yDist)
    }

    val currentPosition: Point2D
        get() {
            return if(to != null) {
                val percentDone = (now - started) / (estimatedArrival - started)

                val xDist = to!!.x - from.x
                val yDist = to!!.y - from.y

                Point2D.Double(
//                    from.x*(1-percentDone) + to!!.x*percentDone,
//                    from.y*(1-percentDone) + to!!.y*percentDone
                    from.x + percentDone * xDist,
                    from.y + percentDone * yDist
                )

            } else {
                from
            }
        }
}

class Harvester(initialPosition: GridPosition, val gridUnitsPerHour: Double = 0.5) : MovingComponent(initialPosition.mapCoordinates) {
    val state = State(STANDBY)

    val map = get<DepositMap>()
    val base = get<Base>()

    val tank = DepletableResource("tank of $this", 100, 0)

    var currentState
        get() = state.value
        set(newState) {
            state.value = newState
        }

    var currentDeposit: Deposit? = null

    var gridPosition: GridPosition = initialPosition

//    val gridPosition
//        get() = GridPosition(position.x.roundToInt(), position.y.roundToInt())

//    fun SequenceScope<Component>.move(to: GridPosition) = sequence {
//        val route = planRoute(position, Point2D.Double(to.x.toDouble(), to.y.toDouble()))
//
//        route.forEach {
//            hold(1)
//            position = it
//        }
//    }
//
//    fun SequenceScope<Component>.planRoute(from: Point2D, to: Point2D, distancePerTick: Number = 0.1) = sequence {
//        val xDist = to.x - from.x
//        val yDist = to.y - from.y
//
//        val distance = sqrt(xDist * xDist + yDist * yDist)
//        val numMoves = distance / distancePerTick.toDouble()
//
//        val xInc = xDist / numMoves
//        val yInc = yDist / numMoves
//
//        // todo this seems slightly wrong
//        for(i in 0 until floor(numMoves).toInt()) {
//            yield(Point2D.Double(from.x + i * xInc, from.y + i * yInc))
//
//            // to avoid any rounding issues
//            yield(to)
//        }
//    }

    fun searching(): Sequence<Component> = sequence {
        state.value = SCANNING

//        val stepInc = discreteUniform(-3,3)
        val stepInc = discreteUniform(-3, 3)

        while(true) {
            // find a close-by unexplored grid position

            var searchAreaCandiate: GridPosition = gridPosition
            do {
                searchAreaCandiate =
                    map.restrictToMap(searchAreaCandiate.run { copy(x = x + stepInc(), y = y + stepInc()) })
            } while(!base.approveSearchCoordinates(searchAreaCandiate))

            move(searchAreaCandiate.mapCoordinates, gridUnitsPerHour)

            // now start the scanning process
            currentState = SCANNING
            hold(10.hours, "scanning")

            // did we find a deposit
            map.deposits.find { depost ->
                depost.gridPosition == gridPosition
            }?.also { deposit ->
                // report to base
                base.registerDeposit(deposit)
                currentDeposit = deposit

                // todo https://github.com/holgerbrandl/kalasim/issues/37
//                yield(activate(process = Harvester::harvesting))
                activate(process = Harvester::harvesting)
            }
        }
    }

    fun unload() = sequence {
        currentState = UNLOADING
        move(base.position.mapCoordinates, gridUnitsPerHour)
        // unloading time correlates with load status
        hold(tank.level / 10, "unloading ${tank.level} hydrat units")
        put(get<Base>().refinery withQuantity tank.level)

        // empty the tank
        take(tank, tank.level)
    }

    fun harvesting(): Sequence<Component> = sequence {
        require(currentDeposit != null) { "deposit must be set when harvesting" }

        if(currentDeposit == null) {
            currentDeposit = get<Base>().requestAssignment(this@Harvester)
        }

        if(currentDeposit == null) {
            // todo this is not pretty, the user must never yield herself
            activate(process = Harvester::searching)
        }

        state.value = MINING

        while(!currentDeposit!!.isDepleted && tank.occupancy < 1.0) {
            hold(1)
            request(currentDeposit!!)
        }

        if(tank.isFull) {
            activate(process = Harvester::unload)
        } else {
            require(currentDeposit!!.isDepleted)
            currentDeposit = null
            activate(process = Harvester::harvesting)
        }
    }
}

class Base : Component() {
    val position: GridPosition = GridPosition(40, 40)

    val knownDeposits = mutableListOf<Deposit>()

    // a list of previsously scanned positions
    private val scanHistory = mutableMapOf<GridPosition, TickTime>()

    val refinery = DepletableResource(capacity = Double.MAX_VALUE, initialLevel = 0)

    fun registerDeposit(deposit: Deposit) {
        knownDeposits.add(deposit)
    }

    /** Performs analysis to find a suitable deposit for harvesting for the given harvester. */
    fun requestAssignment(harvester: Harvester): Deposit? {
        return knownDeposits.filter { !it.isDepleted }.ifEmpty { null }?.sortedBy {
            it.gridPosition.distance(harvester.gridPosition)
        }?.firstOrNull()
    }


    /** Reject search position if they have been scanned already*/
    fun approveSearchCoordinates(searchPosition: GridPosition): Boolean {
        val unexplored = scanHistory.containsKey(searchPosition)

        if(unexplored) {
            scanHistory[searchPosition] = now
        }

        return !unexplored
    }
}

class HydProd : Environment(true) {
    init {
        tickTransform = TickTransform(TimeUnit.MINUTES)
    }

    // the initially unknown list of deposits
    val map = dependency { DepositMap() }
    val base = dependency { Base() }
    val harvesters = List(4) { Harvester(base.position) }
}

fun main() {
    val prod = HydProd()
    prod.run(1000)

    // analyze production
    println("produced hydrate units: ${prod.base.refinery.level}")
}