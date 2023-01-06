package org.kalasim.examples.taxiinc.vehiclerouting

import org.kalasim.examples.taxiinc.vehiclerouting.domain.Customer
import org.kalasim.examples.taxiinc.vehiclerouting.domain.Depot
import org.kalasim.examples.taxiinc.vehiclerouting.domain.Vehicle
import org.kalasim.examples.taxiinc.vehiclerouting.domain.VehicleRoutingSolution
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.AirLocation
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.DistanceType
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.config.solver.EnvironmentMode
import org.optaplanner.core.config.solver.SolverConfig
import org.optaplanner.core.config.solver.termination.TerminationConfig
import java.io.File
import kotlin.random.Random

fun main() {
    val solverConfig =
//        SolverConfig.createFromXmlResource("vehicleRoutingSolverConfig.xml")
        SolverConfig.createFromXmlFile(File("src/main/resources/vehicleRoutingSolverConfig.xml"))
    solverConfig.withEnvironmentMode(EnvironmentMode.REPRODUCIBLE)
//        .withMoveThreadCount(org.optaplanner.examples.vehiclerouting.app.VehicleRoutingMultiThreadedReproducibilityTest.MOVE_THREAD_COUNT)
//    solverConfig.phaseConfigList.forEach(Consumer { phaseConfig: PhaseConfig<*> ->
//        if(LocalSearchPhaseConfig::class.java.isAssignableFrom(phaseConfig.javaClass)) {
//            phaseConfig.terminationConfig =
//                TerminationConfig().withStepCountLimit(org.optaplanner.examples.vehiclerouting.app.VehicleRoutingMultiThreadedReproducibilityTest.STEP_LIMIT)
//        }
//    })
        .withTerminationConfig(TerminationConfig().withSecondsSpentLimit(20))

    val solverFactory = SolverFactory.create<VehicleRoutingSolution>(solverConfig)
    val solver = solverFactory.buildSolver()

    val problem = VehicleRoutingSolution().apply {
        val r = Random(32)

        distanceType = DistanceType.AIR_DISTANCE
        distanceUnitOfMeasurement = "km"

        locationList = List(10) { AirLocation(it.toLong(), r.nextDouble(10.0), r.nextDouble(10.0)) }
        depotList = listOf(Depot(1L, AirLocation(1L, 0.0, 0.0)))

        vehicleList = List(3) { Vehicle(it.toLong(), 4, depotList.first()) }

        customerList = List(10) { Customer(it.toLong(), locationList.random(r), r.nextInt(2)) }
    }

    val solution = solver.solve(problem)

    println(solution)
}
