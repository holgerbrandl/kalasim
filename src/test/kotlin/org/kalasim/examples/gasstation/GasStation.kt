@file:Suppress("MayBeConstant")

package org.kalasim.examples.gasstation

import org.kalasim.*
import org.kalasim.misc.printThis
import org.kalasim.monitors.printHistogram
import org.kalasim.plot.kravis.display
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * A car arrives at the gas station for refueling.
 *
 * It requests one of the gas station's fuel pumps and tries to get the
 * desired amount of gas from it. If the stations reservoir is
 * depleted, the car has to wait for the tank truck to arrive.
 */

// based on SimPy example model
val GAS_STATION_SIZE = 200.0  // liters
val THRESHOLD = 25.0  // Threshold for calling the tank truck (in %)
val FUEL_TANK_SIZE = 50.0  // liters
val FUEL_TANK_LEVEL_RANGE = 5.. 25
val REFUELING_SPEED = 2.0  // liters / second
val TANK_TRUCK_TIME = 300.0  // Seconds it takes the tank truck to arrive
val INTER_ARRIVAL_TIME_RANGE = 10..100  // Create a car every [min, max] seconds
val SIM_TIME = 20000.0  // Simulation time in seconds

// todo review status of https://youtrack.jetbrains.com/issue/KT-50586

private const val FUEL_TANK = "fuel_pump"

/** Arrives at the gas station after a certain delay and refuels it.*/
class TankTruck : Component() {
    val fuelPump: DepletableResource by inject(qualifier = named(FUEL_TANK))

    val unloaded = State(false)

    override fun process() = sequence {
        hold(TANK_TRUCK_TIME)

        // fill but cap when tank is full
//        put(fuelPump, quantity = GAS_STATION_SIZE, capacityLimitMode = CapacityLimitMode.CAP)

        // same effect, but different approach is to refill the missing quantity
        put(fuelPump, quantity = fuelPump.capacity - fuelPump.level)
        unloaded.value = true
    }
}

/** A car arrives at the gas station for refueling.
*
* It requests one of the gas station's fuel pumps and tries to get the
* desired amount of gas from it. If the stations reservoir is
* depleted, the car has to wait for the tank truck to arrive.
*/
class Car(
    val tankSize: Double = FUEL_TANK_SIZE,
) : Component() {

    // Sample an initial level
    val fuelTankLevel = discreteUniform(FUEL_TANK_LEVEL_RANGE)()

    // Resolve dependencies
    val fuelPump = get<Resource>()
    val stationTank: DepletableResource by inject(qualifier = named(FUEL_TANK))

    override fun process() = sequence {
        request(fuelPump, description = "waiting for free pump") {
            val litersRequired = tankSize - fuelTankLevel

            take(stationTank, quantity = litersRequired)
            hold(litersRequired / REFUELING_SPEED)
            println("finished $name")
        }
    }
}


class GasStation : Environment(enableConsoleLogger = true) {
    val tank = dependency(qualifier = named(FUEL_TANK)) { DepletableResource(FUEL_TANK, GAS_STATION_SIZE) }

    val fuelPumps = dependency { Resource(capacity = 2) }

    init {
        // Generate new cars that arrive at the gas station.
        ComponentGenerator(iat = with(INTER_ARRIVAL_TIME_RANGE) { uniform(first, last) }) { Car() }

        //Periodically check the level of the *fuel_pump* and call the tank truck if the level falls below a threshold.
        object : Component("gas_station_control") {
            override fun repeatedProcess() = sequence {
                // Order a new truck if the fuel-pump runs of out fuel
                if(tank.level / tank.capacity * 100 < THRESHOLD) {
                    log("Running out of fuel (remaining ${tank.level}). Ordering new fuel truck...")
                    wait(TankTruck().unloaded, true)
                }

                hold(10) // check every 10 seconds
            }
        }
    }
}

fun main() {

    val gasStation = GasStation()

    gasStation.run(SIM_TIME)

    // use dependency lookup to get tank
    //val tank = sim.get<DepletableResource>(qualifier = named(FUEL_PUMP))

    // or accessor
    val tank = gasStation.tank

    // print some stats
    tank.levelTimeline.printHistogram()

    tank.levelTimeline.display().show()
//    claimedQuantityMonitor.display()
//    availableQuantityMonitor.display()

    gasStation.fuelPumps.claimedTimeline.printHistogram()
    gasStation.fuelPumps.requesters.queueLengthTimeline.printThis()
    gasStation.fuelPumps.requesters.lengthOfStayStatistics.printThis()
}