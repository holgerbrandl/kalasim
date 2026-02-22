@file:OptIn(AmbiguousDuration::class)

package org.kalasim.benchmarks

import com.systema.aps.scheduler.now
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.kalasim.exponential
import org.kalasim.examples.MMcQueue
import org.kalasim.examples.er.EmergencyRoom
import org.kalasim.examples.er.FifoNurse
import org.kalasim.examples.er.RefittingAvoidanceNurse
import org.kalasim.examples.er.UrgencyNurse
import org.kalasim.examples.shipyard.Shipyard
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.weeks
import util.ProdSim
import util.largeModel
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.measureTime


// test including scheduling problem conversion
object SimpleBenchmark {
    val logger = KotlinLogging.logger {}

    @JvmStatic
    fun main(args: Array<String>) {

        val config = "baseline4" // new baseline from SYSTEMA_FOP_2035

        val now = now()
        val date = SimpleDateFormat("yyyyMMdd").format(Date())

        data class Measurement(
            val scenario: String,
            val timestamp: Instant,
//            val run: String,
            val runIdx: Int,
            val runtime: Int,
        )

        val outputFile = Path.of("modules") / "benchmarks" / "simple_perf_logs" / "simplebench_${date}_${config}.csv"
        require(!outputFile.exists()) { "output file already exists" }


        fun MutableList<Measurement>.runExperiment(name: String, numReplicates: Int = 10, doExperiment: () -> Unit) {
            logger.info { "running experiment $name" }

            //warmup
            doExperiment()

            val measurements = List(numReplicates) { run ->
                val measureTime = measureTime {
                    doExperiment()
                }

                Measurement(name, now, run, measureTime.inWholeMilliseconds.toInt())
            }


            // todo add final run with java21 and alternate garbage collector
            addAll(measurements)
        }

        val measurements = buildList {
            runExperiment("mmc-4-4-12") {
                // Baseline (moderate events, moderate queueing)
                MMcQueue(c = 4, mu = 4, lambda = 12).run(10000000)
            }

            runExperiment("mmc-40-40-800") {
                // High-event-rate, low congestion (throughput stress)
                // Lots of arrivals/services per simulated time (engine does many events), but queues stay small.
                MMcQueue(c = 40, mu = 40, lambda = 800).run(10000000)

            }

            runExperiment("mmc-2-4-7") {
                // Near-saturation, small c (deep queue stress)
                // Fewer servers but very high utilization → long queues, lots of waiting/completions and more state pressure.
                // stresses queue length growth, waiting-time bookkeeping, and edge cases near instability while still stable.
                MMcQueue(c = 2, mu = 4, lambda = 7.6).run(10000000)
            }

            runExperiment("prod4-1h") {
                //BenchmarkUtil.kt
                ProdSim(masterExcel = largeModel).run(5.days)
            }

            runExperiment("prod100-1k") {
                //BenchmarkUtil.kt
                ProdSim(masterExcel = largeModel, useExcelOrders=false, targetWIP = 1000).run(100.days)
            }


            runExperiment("er_small") {
                val er = EmergencyRoom(
                    numRooms = 4,
                    numPhysicians = 6,
                    physicianQualRange = 2..4,
                    patientArrival = exponential(0.2.hours),          // ~ 1 patient / 12 min during day
                    nurse = FifoNurse()
                )
                er.run(10000.days)
            }

            runExperiment("er_hgthpt") {
                // High-throughput (event-rate stress)
                // maximize number of events processed per simulated hour (scheduler/heap pressure), while keeping congestion low by scaling resources up.
                // Why: lots of arrivals + many rooms/doctors → high churn of request/hold/release without the queue exploding
                val er = EmergencyRoom(
                    numRooms = 30,
                    numPhysicians = 60,
                    physicianQualRange = 3..6,
                    patientArrival = exponential(0.02.hours),          // ~ 1 patient / 12 min during day
                    waitingAreaSize = 5000,
                    keepHistory = false,
                    nurse = RefittingAvoidanceNurse
                )
                er.run(1000.days)
            }

            runExperiment("er_nrsat") {
                // Near-saturation / deep-queue (state & queue stress)
                // long waiting line, frequent severity escalations while waiting, more list operations and larger state footprint; still bounded by waitingAreaSize.
                // stresses (a) queue growth, (b) patient escalation timers while waiting, (c) removal from waiting line on death/dispatch, and (d) overall state scale.
                val er = EmergencyRoom(
                    numRooms = 4,
                    numPhysicians = 6,
                    physicianQualRange = 2..4,
                    patientArrival = exponential(0.08.hours),         // ~ 1 patient / 4.8 min during day (~2.5× arrivals)
                    waitingAreaSize = 20000,      // allow deep queue to form
                    keepHistory = false,
                    nurse = UrgencyNurse          // forces more sorting/selection work if you change to sorted structure later
                )
                er.run(1000.days)
            }

            runExperiment("shipyard") {
                Shipyard().apply {
                    configureOrders()
                }.run(52.weeks)
            }
        }

        measurements.toDataFrame().writeCSV(outputFile.toFile())
    }
}
