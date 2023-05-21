package org.kalasim.benchmarks

import org.kalasim.examples.er.EmergencyRoom
import org.openjdk.jmh.annotations.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
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