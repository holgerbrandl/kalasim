package org.github.holgerbrandl.kalasim.examples.kalasim

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import krangl.sleepData
import krangl.sleepPatterns
import kravis.*
import kravis.render.LocalR
import org.apache.commons.math3.distribution.UniformRealDistribution
import org.github.holgerbrandl.kalasim.*
import org.koin.core.get
import org.koin.core.inject
import org.koin.core.qualifier.named

/**
 * A car arrives at the gas station for refueling.
 *
 * It requests one of the gas station's fuel pumps and tries to get the
 * desired amount of gas from it. If the stations reservoir is
 * depleted, the car has to wait for the tank truck to arrive.
 */
object Refuel {

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

    @JvmStatic
    fun main(args: Array<String>) {

        class GasStation : Resource(capacity = 2.0)

        class TankTruck : Component() {
            val fuelPump: Resource by inject(qualifier = named(FUEL_PUMP))

            override suspend fun SequenceScope<Component>.process(it: Component) {
                yield(hold(TANK_TRUCK_TIME))
                val amount = fuelPump.claimedQuantity
                yield(it.put(fuelPump withQuantity amount))
            }
        }

        // we can either inject by constructor...
        class Car(val gasStation: GasStation) : Component() {

            // ... or we can inject by field which is
            // in particular useful for untypes resources and components

            //            val gasStation : Resource by inject(qualifier = named("gas_station"))
            val fuelPump: Resource by inject(qualifier = named(FUEL_PUMP))

            override suspend fun SequenceScope<Component>.process(it: Component) {
                val fuelTankLevel = FUEL_TANK_LEVEL.sample()

                yield(it.request(gasStation))

                val litersRequired = FUEL_TANK_SIZE - fuelTankLevel

                // order a new Tank if the fuelpump runs of out fuel
                if ((fuelPump.availableQuantity() - litersRequired) / fuelPump.capacity * 100 < THRESHOLD) {
                    env.printTrace("running out of fuel at $gasStation. Ordering new fuel truck...")
                    TankTruck()
                }

                yield(request(fuelPump withQuantity litersRequired))
                yield(hold(litersRequired / REFUELING_SPEED))
            }
        }

        createSimulation {

//            single(qualifier = named("gas_station")) { Resource("gas_station", 2) }

            single { GasStation() }

            single(qualifier = named(FUEL_PUMP)) { Resource(FUEL_PUMP, GAS_STATION_SIZE, anonymous = true) }
        }.apply {

            ComponentGenerator(iat = UniformRealDistribution(10.0, 100.0)) { Car(get()) }

            run(SIM_TIME)

            val fuelPump = get<Resource>(qualifier = named(FUEL_PUMP))

            fuelPump.apply {
                capacityMonitor.printStats()
                claimedQuantityMonitor.printStats()
                availableQuantityMonitor.printStats()
            }


            get<GasStation>().requesters.queueLengthStats.println()
            get<GasStation>().requesters.lengthOfStayStats.println()

            // save the simulation state to file
//            Json.encodeToString(this).println()


            get<GasStation>().claimedQuantityMonitor.apply {
                val data = timestamps.zip(values.toList())
                data.plot(
                    x =  Pair<Double,Double>::first,
                    y =  Pair<Double,Double>::second
                ).geomLine().title("gas station claimed capacity").show()
            }

            get<GasStation>().occupancyMonitor.apply {
                val data = timestamps.zip(values.toList())
                data.plot(
                    x =  Pair<Double,Double>::first,
                    y =  Pair<Double,Double>::second
                ).geomLine().title("gas station occupancy").show()
            }
        }
    }
}