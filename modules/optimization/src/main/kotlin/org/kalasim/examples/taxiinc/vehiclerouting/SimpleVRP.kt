package org.kalasim.examples.taxiinc.vehiclerouting

import ai.timefold.solver.core.api.solver.SolverFactory
import ai.timefold.solver.core.config.solver.EnvironmentMode
import ai.timefold.solver.core.config.solver.SolverConfig
import ai.timefold.solver.core.config.solver.termination.TerminationConfig
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.kalasim.examples.taxiinc.vehiclerouting.domain.*
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.AirLocation
import org.kalasim.examples.taxiinc.vehiclerouting.domain.location.DistanceType
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

        locationList = List(10) {
            AirLocation(
                it.toLong(),
                r.nextDouble(10.0),
                r.nextDouble(10.0)
            )
        }
        depotList = listOf(
            Depot(
                1L,
                AirLocation(
                    1L,
                    0.0,
                    0.0
                )
            )
        )

        vehicleList = List(3) {
            Vehicle(
                it.toLong(),
                4,
                depotList.first()
            )
        }

        customerList = List(10) {
            Customer(
                it.toLong(),
                locationList.random(r),
                r.nextInt(2)
            )
        }
    }

    val solution = solver.solve(problem)

    println(solution)
    solution.vehicleList.toDataFrame()
}
