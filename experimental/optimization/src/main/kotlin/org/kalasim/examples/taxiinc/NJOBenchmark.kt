package org.kalasim.examples.taxiinc.opt2

import ai.timefold.solver.benchmark.api.PlannerBenchmark
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory
import ai.timefold.solver.benchmark.config.*
import ai.timefold.solver.benchmark.config.blueprint.SolverBenchmarkBluePrintConfig
import ai.timefold.solver.benchmark.config.blueprint.SolverBenchmarkBluePrintType
import ai.timefold.solver.benchmark.config.statistic.ProblemStatisticType
import ai.timefold.solver.benchmark.config.statistic.SingleStatisticType
import ai.timefold.solver.core.config.solver.EnvironmentMode
import ai.timefold.solver.core.config.solver.SolverConfig
import ai.timefold.solver.core.config.solver.termination.TerminationConfig
import java.io.File


fun main() {
    val config = PlannerBenchmarkConfig().apply {
        benchmarkDirectory = File("benchmark_results").apply {
            if(!exists()) mkdir()
        }
        inheritedSolverBenchmarkConfig = SolverBenchmarkConfig().apply {
            solverConfig = SolverConfig()
                .withMoveThreadCount("")
                .withSolutionClass(TaxiSchedule::class.java)
                .withEntityClasses(Order::class.java)
                .withConstraintProviderClass(ConstraintsProvider::class.java)
                .withEnvironmentMode(EnvironmentMode.FAST_ASSERT)
                .withTerminationConfig(TerminationConfig().withSecondsSpentLimit(10))

            problemBenchmarksConfig= ProblemBenchmarksConfig().apply {
                writeOutputSolutionEnabled  = true
                problemStatisticTypeList = listOf(ProblemStatisticType.BEST_SCORE, ProblemStatisticType.MOVE_COUNT_PER_STEP, )
                singleStatisticTypeList = listOf(SingleStatisticType.CONSTRAINT_MATCH_TOTAL_BEST_SCORE, SingleStatisticType.PICKED_MOVE_TYPE_BEST_SCORE_DIFF)
            }
        }


        solverBenchmarkConfigList = listOf(
            SolverBenchmarkConfig().apply {
                name = "Late Acceptance"

            }
        )

        solverBenchmarkBluePrintConfigList = listOf(SolverBenchmarkBluePrintConfig()
            .withSolverBenchmarkBluePrintType(SolverBenchmarkBluePrintType.EVERY_LOCAL_SEARCH_TYPE)
        )
    }


    val pbf = PlannerBenchmarkFactory.create(config)

    val dataset1 = createSchedule(1, 1)
    val dataset2 = createSchedule(10, 4)
    val dataset3 = createSchedule(30, 8)

    val benchmark: PlannerBenchmark = pbf.buildPlannerBenchmark(
        dataset1, dataset2, dataset3
    )

//    benchmark.

//    benchmark.
    benchmark.benchmarkAndShowReportInBrowser()
}