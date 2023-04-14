package org.kalasim.demo.moon

import org.kalasim.*
import org.kalasim.animation.AnimationComponent
import org.kalasim.misc.roundAny
import org.kalasim.demo.moon.HarvesterState.*
import java.awt.Dimension
import java.awt.geom.Point2D
import java.lang.Math.pow
import kotlin.time.DurationUnit
import kotlin.math.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class HarvesterState { STANDBY, MOVING, SCANNING, MINING, UNLOADING, MAINTENANCE, BROKEN }

class Deposit(val gridPosition: GridPosition, size: Int) : DepletableResource(capacity = size) {
    val miningShaft = Resource("MineShaft[${this}]", 1)
}

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
    val gridDimension: Dimension = Dimension(20, 15)
) : SimulationEntity() {

    // sample deposits
    val deposits = run {
        val capacityDist = uniform(1, maxCapacity)
        // sample deposit location but avoid borders of map (for sake of beauty)
        val x = discreteUniform(1, gridDimension.width - 1)
        val y = discreteUniform(1, gridDimension.height - 1)

        List(numDeposits) {
            Deposit(GridPosition(x(), y()), capacityDist.sample().roundToInt())
        }.groupBy { it.gridPosition }.map { it.value.first() } // remove position duplicates
    }

    fun restrictToMap(gp: GridPosition): GridPosition = GridPosition(
        max(min(gp.x, gridDimension.width), 1),
        max(min(gp.y, gridDimension.height), 1)
    )

    val depletionRatio
        get() = 1 - deposits.map { it.level to it.capacity }.run { sumOf { it.first } / sumOf { it.second } }
            .roundAny(2)
}


class Harvester(initialPosition: GridPosition, private val gridUnitsPerHour: Double = 0.5) :
    AnimationComponent(initialPosition.mapCoordinates, process = Harvester::searching) {

    val state = State(STANDBY)

    private val map = get<DepositMap>()
    private val base = get<Base>()

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
                if(currentDeposit != null) {
                    activate(process = Harvester::harvesting)
                }
            }

            base.reportScanCompleted(searchAreaCandiate)
        }
    }


    private suspend fun SequenceScope<Component>.moveTo(gridPosition: GridPosition, description: String? = null) {
        currentState = MOVING
        move(gridPosition.mapCoordinates, gridUnitsPerHour, description = description)
        this@Harvester.gridPosition = gridPosition
    }

    fun unload() = sequence {
        moveTo(base.position)

        val unloadingUnitsPerHours = 20  // speed of unloading

        // unloading time correlates with load status
        currentState = UNLOADING
        hold((tank.level / unloadingUnitsPerHours).roundToInt().hours, "Unloading ${tank.level} water units")
        put(get<Base>().refinery, tank.level)

        // empty the tank
        take(tank, tank.level)

        activate(process = Harvester::harvesting)
    }

    fun harvesting(): Sequence<Component> = sequence {
        if(currentDeposit == null) {
            currentDeposit = get<Base>().requestAssignment(this@Harvester)
        }

        if(currentDeposit == null) {
            activate(process = Harvester::searching)
        }

        moveTo(currentDeposit!!.gridPosition)


        // MODEL 1: Mine increments: This allows for better progress monitoring in the UI, but is overly
        // complex from a modelling and event perspective
        // Could we avoid the loop by using process interaction here? --> yes use mine-shafts see below
        val miningUnitsPerHour = 15.0

        request(currentDeposit!!.miningShaft) {
            while(!currentDeposit!!.isDepleted && !tank.isFull) {
                val quantity = min(miningUnitsPerHour / 4, currentDeposit!!.level)
                take(currentDeposit!!, quantity, failDelay = 0.seconds)
                if(failed) { // could happen if other harvester tries to mine here as well
                    break
                }

                currentState = MINING
                hold(15.minutes)
                put(tank, quantity, capacityLimitMode = CapacityLimitMode.CAP)
            }
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

class Base : Component(process=Base::consumeWater) {
    val position: GridPosition = GridPosition(5, 8)

    init {
        require(get<DepositMap>().restrictToMap(position) == position) { "base out of map" }
    }

    val knownDeposits = mutableListOf<Deposit>()

    // a list of previously scanned positions
    val scanHistory = mutableMapOf<GridPosition, TickTime>()

    val refinery = DepletableResource(capacity = Int.MAX_VALUE, initialLevel = 100)

    fun registerDeposit(deposit: Deposit) {
        knownDeposits.add(deposit)
    }

    val waterConsumption = exponential(3)

    // water consumption of the base
    fun consumeWater() = sequence {
        while(true) {
            hold(1.hours)
            take(refinery, quantity = min(refinery.level, waterConsumption()))
        }
    }

    /** Performs analysis to find a suitable deposit for harvesting for the given harvester. */
    fun requestAssignment(harvester: Harvester): Deposit? {
        val candidate = knownDeposits
            .filter { !it.isDepleted }
            .filter { it.miningShaft.occupancy < 1 }
            .ifEmpty { null }?.minByOrNull {
                it.gridPosition.distance(harvester.gridPosition)
            }
        return if(candidate != null && candidate.gridPosition.distance(harvester.gridPosition) < 4.0) candidate else null
    }


    /** Reject search position if they have been scanned already*/
    fun approveSearchCoordinates(searchPosition: GridPosition): Boolean {
        return !scanHistory.containsKey(searchPosition)
    }

    fun reportScanCompleted(searchPosition: GridPosition) {
        scanHistory[searchPosition] = now
    }
}

class LunarMining(
    numHarvesters: Int = 5,
    numDeposits: Int = 10,
    logEvents: Boolean = true,
    seed: Int = Defaults.DEFAULT_SEED
) : Environment(enableConsoleLogger = logEvents, randomSeed = seed) {
    // the initially unknown list of deposits
    val map = dependency { DepositMap(numDeposits = numDeposits) }
    val base = dependency { Base() }
    val harvesters = List(numHarvesters) { Harvester(base.position) }
}

fun main() {
    val prod = LunarMining()

    prod.run(60.days)

    // Analyze production KPIs
    println("Produced water units: ${prod.base.refinery.level}")
    println("Deposit depletion ${prod.map.depletionRatio}%")
}

