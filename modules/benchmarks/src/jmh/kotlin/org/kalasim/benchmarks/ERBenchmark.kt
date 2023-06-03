package org.kalasim.benchmarks

import org.kalasim.examples.er.EmergencyRoom
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
open class ERBenchmark {

//    var value: Double = 0.0
//
//    @Setup
//    fun setUp(): Unit {
//        value = 3.0
//    }

    @Benchmark
    fun measureER() {
        val emergencyRoom = EmergencyRoom()

        emergencyRoom.run(30.days)
    }

}