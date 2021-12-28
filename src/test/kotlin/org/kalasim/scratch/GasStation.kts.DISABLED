@file:Suppress("PropertyName", "PrivatePropertyName")

package org.kalasim.scratch

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.kalasim.*
import org.kalasim.examples.GasStation
import org.kalasim.monitors.printHistogram
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
val FUEL_TANK_LEVEL = UniformRealDistribution(5.0, 25.0) // Min/max levels of fuel tanks (in liters)
val REFUELING_SPEED = 2.0  // liters / second
val TANK_TRUCK_TIME = 300.0  // Seconds it takes the tank truck to arrive
val T_INTER = UniformRealDistribution(10.0, 100.0)  // Create a car every [min, max] seconds

//    val SIM_TIME = 200000.0  // Original Simulation time in seconds
val SIM_TIME = 20000.0  // Simulation time in seconds


private val FUEL_PUMP = "fuel_pump"


class TankTruck : Component() {
    val fuelPump: DepletableResource by inject(qualifier = named(FUEL_PUMP))

    override fun process() = sequence {
        hold(TANK_TRUCK_TIME)

        // fill but cap when tank is full
//        put(fuelPump, quantity = GAS_STATION_SIZE, capacityLimitMode = CapacityLimitMode.CAP)

        // same effect, but different approach is to refill the missing quantity
        put(fuelPump, quantity = fuelPump.capacity - fuelPump.level)
    }
}

// we can either inject by constructor...
class Car(
    val tankSize: Double = FUEL_TANK_SIZE,
) : Component() {

    // sample an initial level
    val fuelTankLevel = FUEL_TANK_LEVEL.sample()

    // dependencies
    val fuelPump = get<Resource>()
//    val stationTank: DepletableResource by inject(qualifier = named(FUEL_PUMP))
    val stationTank = get<DepletableResource>()

   override fun process() = sequence<Component> {
       // find a free pump
       request(fuelPump) {

           val litersRequired = tankSize - fuelTankLevel

           // Order a new Tank if the fuel-pump runs of out fuel
           if((fuelPump.availableQuantity - litersRequired) / fuelPump.capacity * 100 < THRESHOLD) {
               log("running out of fuel at $gasStation. Ordering new fuel truck...")
               TankTruck()
           }

           take(stationTank, quantity = litersRequired)
           hold(litersRequired / REFUELING_SPEED)
       }
   }
}


class GasStation : Environment() {
    val tank = dependency(qualifier = named(FUEL_PUMP)) { DepletableResource(FUEL_PUMP, GAS_STATION_SIZE) }

    val fuelPumps = dependency { Resource(capacity = 2) }

    init {
        // spin up sub-process that will produce cars
        ComponentGenerator(iat = T_INTER) { Car(get()) }

        // todo test this
//        dependency { this } // simplify dependency injection
    }
}

val gasStation = GasStation()

gasStation.run(SIM_TIME)

// use dependency lookup to get tank
//val tank = sim.get<DepletableResource>(qualifier = named(FUEL_PUMP))

// or accessor
val tank = gasStation.tank

// print some stats
tank.capacityTimeline.printHistogram()
tank.claimedTimeline.printHistogram()
tank.availabilityTimeline.printHistogram()

//                capacityTimeline.display()
//                claimedQuantityMonitor.display()
//                availableQuantityMonitor.display()


//https://youtrack.jetbrains.com/issue/KT-50566
gasStation.fuelPumps.requesters.queueLengthTimeline.let{ println(it)}
gasStation.fuelPumps.requesters.lengthOfStayTimeline.let{ println(it)}


