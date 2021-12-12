package org.kalasim.sims.hydprod

import org.kalasim.*
import org.kalasim.sims.hydprod.HarvesterState.*
import java.awt.geom.Point2D
import kotlin.math.*

enum class HarvesterState { STANDBY, MOVING, SCANNING, MINING, UNLOADING, MAINTENANCE, BROKEN }

class Deposit(val gridPosition: GridPosition, size: Int) : DepletableResource(capacity = size)

data class GridPosition(val x: Int, val y: Int)

fun GridPosition.distance(other: GridPosition) =
    sqrt(Math.pow((x - other.x).toDouble(), 2.0) - Math.pow((y - other.y).toDouble(), 2.0))

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
    val x = discreteUniform(lowerLeft.x,upperRight.x)
    val y = discreteUniform(lowerLeft.y,upperRight.y)

    val deposits = List(numDeposits) {
        Deposit(GridPosition(x(), y()), capacityDist.sample().roundToInt())
    }.groupBy { it.gridPosition }.map { it.value.first() } // remove position duplicates

    fun isOnMap(gp: GridPosition): Boolean = TODO()

    fun restrictToMap(gp: GridPosition): GridPosition = GridPosition(
        min(max(gp.x, lowerLeft.x), upperRight.x),
        min(max(gp.y, lowerLeft.y), upperRight.y)
    )
}

class Harvester : Component() {

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

    lateinit var position: Point2D
    val gridPosition
        get() = GridPosition(position.x.roundToInt(), position.y.roundToInt())

    fun SequenceScope<Component>.move(to: GridPosition) = sequence {
        val route = planRoute(position, Point2D.Double(to.x.toDouble(), to.y.toDouble()))

        route.forEach {
            hold(1)
            position = it
        }
    }

    fun SequenceScope<Component>.planRoute(from: Point2D, to: Point2D, distancePerTick: Number = 0.1) = sequence {
        val xDist = to.x - from.x
        val yDist = to.y - from.y

        val distance = sqrt(xDist * xDist + yDist * yDist)
        val numMoves = distance / distancePerTick.toDouble()

        val xInc = xDist / numMoves
        val yInc = yDist / numMoves

        // todo this seems slightly wrong
        for(i in 0 until floor(numMoves).toInt()) {
            yield(Point2D.Double(from.x + i * xInc, from.y + i * yInc))

            // to avoid any rounding issues
            yield(to)
        }
    }

    fun searching() = sequence {
        state.value = SCANNING

//        val stepInc = discreteUniform(-3,3)
        val stepInc = enumerated(-3 .. 3)

        while(true) {
            // find a close-by unexplored grid position

            var searchAreaCandiate: GridPosition = gridPosition
            do {
                searchAreaCandiate =
                    map.restrictToMap(searchAreaCandiate.run { copy(x = x + stepInc(), y = y + stepInc()) })
            } while(!base.approveSearchCoordinates(searchAreaCandiate))

            move(searchAreaCandiate)

            // now start the scanning process
            currentState = SCANNING
            hold(10, "scanning")

            // did we find a deposit
            map.deposits.find { depost ->
                depost.gridPosition == gridPosition
            }?.also { deposit ->
                // report to base
                base.registerDeposit(deposit)
                currentDeposit = deposit

                // todo https://github.com/holgerbrandl/kalasim/issues/37
                yield(activate(process = Harvester::harvesting))
            }
        }
    }

    fun unload() = sequence {
        currentState = UNLOADING
        move(base.position)
        // unloading time correlates with load status
        hold(tank.level / 10, "unloading ${tank.level} hydrat units")
        put(get<Base>().refinery withQuantity tank.level)
        // empty the tank
        tank.level = 0.0
    }

    fun harvesting(): Sequence<Component> = sequence {
        require(currentDeposit != null) { "deposit must be set when harvesting" }

        if(currentDeposit == null) {
            currentDeposit = get<Base>().requestAssignment(this@Harvester)
        }

        if(currentDeposit == null) {
            // todo this is not pretty, the user must never yield herself
            yield(activate(process = Harvester::searching))
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
            yield(activate(process = Harvester::harvesting))
        }
    }
}

class Base : Component() {
    val position: GridPosition = GridPosition(40, 40)

    val knownDeposits = mutableListOf<Deposit>()

    // a list of previsously scanned positions
    private val scanHistory = mutableMapOf<GridPosition, TickTime>()

    val refinery = DepletableResource(capacity = Double.MAX_VALUE, initialLevel = 0)

    fun registerDeposit(deposit: Deposit) { knownDeposits.add(deposit) }

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
    // the initially unknown list of deposits
    val deposits = dependency {  DepositMap()}
    val base = dependency { Base() }
    val harvesters = repeat(4) { Harvester() }
}

fun main() {
    val prod = HydProd()
    prod.run(1000)

    // analyze production
    println("produced hydrate units: ${prod.base.refinery.level}")
}