@file:OptIn(AmbiguousDuration::class)

package org.kalasim.benchmarks

import com.systema.aps.scheduler.now
import com.systema.aps.simulation.BaseFactory
import com.systema.aps.simulation.fullFabMesXls
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.examples.MMcQueue
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.examples.shipyard.Shipyard
import org.kalasim.misc.AmbiguousDuration
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.days
import kotlin.time.measureTime


// test including scheduling problem conversion
object SimpleBenchmark {
    @JvmStatic
    fun main(args: Array<String>) {

        val config = "baseline" // new baseline from SYSTEMA_FOP_2035

        val now = now()
        val date = SimpleDateFormat("yyyyMMdd").format(Date())

        data class Measurement(
            val scenario: String,
            val timestamp: Instant,
//            val run: String,
            val runIdx: Int,
            val runtime: Int,
        )

        val outputFile = Path.of("simplebench_${date}_${config}.csv")
        require(!outputFile.exists()) { "output file already exists" }



        fun MutableList<Measurement>.runExperiment(name: String, numReplicates: Int = 5, doExperiment: () -> Unit) {
            val measurements = List(10) { run ->
                val measureTime = measureTime {
                    doExperiment()
                }

                Measurement(name, now, run, measureTime.inWholeMilliseconds.toInt())
            }


            // todo add final run with java21 and alternate garbage collector
            addAll(measurements)
        }

        val measurements = buildList {
            runExperiment("mmc-2-4-12") {
                MMcQueue(c = 2, mu = 4, lambda = 12).run(1000)
            }
            runExperiment("mmc-4-4-12") {
                MMcQueue(c = 4, mu = 4, lambda = 12).run(1000)
            }

            runExperiment("prod4") {
                BaseFactory(masterExcel = fullFabMesXls).run(4.days)
            }
            runExperiment("er365") {
                EmergencyRoom().run(365.days)
            }
            runExperiment("er10k") {
                EmergencyRoom().run(10000.days)
            }
            runExperiment("shipyard") {
                Shipyard().run(100.days)
            }
        }

        measurements.toDataFrame().writeCSV(outputFile.toFile())
    }
}
