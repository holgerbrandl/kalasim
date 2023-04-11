package org.kalasim.demo.moon

//import kravis.*
import kotlin.time.Duration.Companion.hours

fun main() {
    val sims = List(10) { numHarvesters ->
        List(100) {
            LunarMining(numHarvesters, logEvents=false).apply {
                run(40.hours)
            }
        }
    }.flatten()

    // visualize timeline

//    sims.withIndex().map { (idx, sim) ->
//        sim.harvesters.size to sim.base.refinery.levelTimeline.statistics().min
//    }.plot(x={ first}, y={second}).geomBoxplot()

//    val waterSupply = sims.withIndex().map { (idx, sim) ->
//        sim.base.refinery.levelTimeline//.statistics()
//            .stepFun()
//            .toDataFrame()
//            .addColumn("num_harvesters") { sim.harvesters.size }
//            .addColumn("run") { idx }
//    }.concat()
//
//    // todo bring back group for correct vis
//    waterSupply
//        .plot(x = "time", y = "value", group="run")
//        .geomLine(color = RColor.red, alpha = .2)
//        .facetWrap("num_harvesters", ncol = 1)
//        .show()


    // Analyze production KPIs
//    println("Produced water units: ${prod.base.refinery.level}")
//    println("Deposit depletion ${prod.map.depletionRatio}%")
}

