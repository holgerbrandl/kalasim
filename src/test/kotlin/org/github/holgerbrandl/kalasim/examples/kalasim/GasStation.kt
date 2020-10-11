package org.github.holgerbrandl.kalasim.examples.kalasim

import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.kalasim.Component
import org.github.holgerbrandl.kalasim.Resource
import org.github.holgerbrandl.kalasim.examples.koiner.createSimulation
import org.koin.core.qualifier.named

/** A car arrives at the gas station for refueling.

It requests one of the gas station's fuel pumps and tries to get the
desired amount of gas from it. If the stations reservoir is
depleted, the car has to wait for the tank truck to arrive.
 */


// based on SimPy example model
val GAS_STATION_SIZE = 200  // liters
val THRESHOLD = 25.0  // Threshold for calling the tank truck (in %)
val FUEL_TANK_SIZE = 50.0  // liters
val FUEL_TANK_LEVEL = UniformRealDistribution(5.0, 25.0) // Min/max levels of fuel tanks (in liters)
val REFUELING_SPEED = 2.0  // liters / second
val TANK_TRUCK_TIME = 300.0  // Seconds it takes the tank truck to arrive
val T_INTER = UniformRealDistribution(10.0, 100.0)  // Create a car every [min, max] seconds
val SIM_TIME = 200000  // Simulation time in seconds


object GasStation {

    class Car(val gasStation: Resource) : Component() {
        override suspend fun SequenceScope<Component>.process(it: Component) {
            val fuelTankLevel = FUEL_TANK_LEVEL.sample()

            // TODO finish example implementatoin here
//            yield(it.request(gasStation))

            val litersRequired = FUEL_TANK_SIZE - fuelTankLevel
        }

    }

    @JvmStatic
    fun main(args: Array<String>) {
        createSimulation {
//            single { Resource("gas_station", 2) }
            single(qualifier = named("gas_station")) { Resource("gas_station", 2) }

            single(qualifier = named("fuel_pump")) { Resource("fuel_pump", GAS_STATION_SIZE, anonymous = true) }
        }
    }
}