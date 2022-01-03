@file:Suppress("MemberVisibilityCanBePrivate")

package org.kalasim.sims.hydprod

import org.kalasim.*
import org.kalasim.misc.roundAny
import org.kalasim.sims.hydprod.HarvesterState.*
import java.awt.Dimension
import java.awt.geom.Point2D
import java.lang.Math.pow
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


enum class HarvesterState { STANDBY, MOVING, SCANNING, MINING, UNLOADING, MAINTENANCE, BROKEN }

class Deposit(val gridPosition: GridPosition, size: Int) : DepletableResource(capacity = size)


/** A discrete position on the surface */
data class GridPosition(val x: Int, val y: Int) {
    val mapCoordinates: Point2D
        get() = Point2D.Double(x * 10.0, y * 10.0)

    fun distance(other: GridPosition) =
        sqrt(pow((x - other.x).toDouble(), 2.0) - pow((y - other.y).toDouble(), 2.0))
}


class DepositMap(
    numDeposits: Int = 10,
    maxCapacity: Int = 1000,
    val gridDimension: Dimension = Dimension(30, 30)
) : SimulationEntity() {

    // sample deposits
    val deposits = run {
        val capacityDist = uniform(1, maxCapacity)
        val x = discreteUniform(0, gridDimension.width)
        val y = discreteUniform(0, gridDimension.height)

        List(numDeposits) {
            Deposit(GridPosition(x(), y()), capacityDist.sample().roundToInt())
        }.groupBy { it.gridPosition }.map { it.value.first() } // remove position duplicates
    }

    fun restrictToMap(gp: GridPosition): GridPosition = GridPosition(
        max(min(gp.x, gridDimension.width), 0),
        max(min(gp.y, gridDimension.height), 0)
    )
}


class Harvester(initialPosition: GridPosition, val gridUnitsPerHour: Double = 0.5) :
    MovingComponent(initialPosition.mapCoordinates, process = Harvester::searching) {

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


    fun searching(): Sequence<Component> = sequence {
        val stepInc = discreteUniform(-3, 3)

        while(true) {
            // find a close-by unexplored grid position
            var searchAreaCandiate: GridPosition = gridPosition
            do {
                searchAreaCandiate =
                    map.restrictToMap(searchAreaCandiate.run { copy(x = x + stepInc(), y = y + stepInc()) })
            } while(!base.approveSearchCoordinates(searchAreaCandiate))

            moveTo(searchAreaCandiate)

            // now start the scanning process
            currentState = SCANNING
            hold(1.hours, "scanning")

            // did we find a deposit
            map.deposits.find { depost ->
                depost.gridPosition == gridPosition
            }?.also { deposit ->
                // Report new deposit to base
                base.registerDeposit(deposit)
                currentDeposit = deposit

                activate(process = Harvester::harvesting)
            }

            // can base assign a close-by one?
            if(currentDeposit == null) {
                currentDeposit = get<Base>().requestAssignment(this@Harvester)
                if(currentDeposit!=null){
                    activate(process = Harvester::harvesting)
                }
            }
        }
    }


    private suspend fun SequenceScope<Component>.moveTo(gridPosition: GridPosition, description: String? = null) {
        currentState = MOVING
        move(gridPosition.mapCoordinates, gridUnitsPerHour, description = description)
        this@Harvester.gridPosition = gridPosition
    }

    fun unload() = sequence {
        moveTo(base.position)

        val unloadUnitsPerMinute = 0.5  // speed of unloading

        // unloading time correlates with load status
        currentState = UNLOADING
        hold((tank.level * unloadUnitsPerMinute).roundToInt().minutes, "unloading ${tank.level} hydrat units")
        put(get<Base>().refinery, tank.level)

        // empty the tank
        take(tank, tank.level)

        activate(process = Harvester::harvesting)
    }

    fun harvesting(): Sequence<Component> = sequence {
//        require(currentDeposit != null) { "deposit must be set when harvesting" }

        if(currentDeposit == null) {
            currentDeposit = get<Base>().requestAssignment(this@Harvester)
        }

        if(currentDeposit == null) {
            activate(process = Harvester::searching)
        }

        moveTo(currentDeposit!!.gridPosition)

        //todo could we avoid the loop by using process interaction here?
        val miningUnitsPerHour = 5.0
        while(!currentDeposit!!.isDepleted && !tank.isFull) {
            val quantity = min(miningUnitsPerHour, currentDeposit!!.level)
            take(currentDeposit!!, quantity, failDelay = 0)
            if(failed) { // could happen if other harvester tries to mine here as well
                break
            }

            currentState = MINING
            hold(1.hours)

            put(tank, quantity, capacityLimitMode = CapacityLimitMode.CAP)
        }

        if(tank.isFull) {
            activate(process = Harvester::unload)
        } else {
            require(currentDeposit!!.isDepleted)
            currentDeposit = null
            activate(process = Harvester::harvesting)
        }

        require(false)
    }
}

class Base : Component() {
    val position: GridPosition = GridPosition(10, 15)

    init{
        require(get<DepositMap>().restrictToMap(position) == position){ "base out of map" }
    }

    val knownDeposits = mutableListOf<Deposit>()

    // a list of previously scanned positions
    private val scanHistory = mutableMapOf<GridPosition, TickTime>()

    val refinery = DepletableResource(capacity = Int.MAX_VALUE, initialLevel = 0)

    fun registerDeposit(deposit: Deposit) {
        knownDeposits.add(deposit)
    }

    /** Performs analysis to find a suitable deposit for harvesting for the given harvester. */
    fun requestAssignment(harvester: Harvester): Deposit? {
        return knownDeposits.filter { !it.isDepleted }.ifEmpty { null }?.minByOrNull {
            it.gridPosition.distance(harvester.gridPosition)
        }
    }


    /** Reject search position if they have been scanned already*/
    fun approveSearchCoordinates(searchPosition: GridPosition): Boolean {
        val unexplored = !scanHistory.containsKey(searchPosition)

        if(unexplored) {
            val mapSize = get<DepositMap>().gridDimension
            println("map coverage increased to ${(scanHistory.size.toDouble() / (mapSize.height * mapSize.width)).roundAny(2)}")

            scanHistory[searchPosition] = now
        }

        return unexplored
    }
}

class HydProd : Environment(true) {
    init {
        tickTransform = TickTransform(TimeUnit.MINUTES)
    }

    // the initially unknown list of deposits
    val map = dependency { DepositMap() }
    val base = dependency { Base() }
    val harvesters = List(1) { Harvester(base.position) }
}

fun main() {
    val prod = HydProd()
    prod.run(60.days)

    // analyze production
    println("produced hydrate units: ${prod.base.refinery.level}")

    val depletionRatio = prod.map.deposits.map { it.level to it.capacity }.run { sumOf { it.first } / sumOf { it.second } }
    println("Deposit depletion ${depletionRatio.roundAny(2)} *")
}