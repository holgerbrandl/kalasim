package org.kalasim.benchmarks

import org.kalasim.examples.shipyard.Shipyard
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

//@State(Scope.Benchmark)
//@Fork(4)
@Warmup(iterations = 1)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
open class ShipyardBenchmark {

//    @State(Scope.Benchmark)
//    open class ExecutionPlan {
//
//        @Param("10", "20", "30")
//        var numFloors: Int = 0
//
////        lateinit var colData: DoubleArray
////        @Setup(Level.Invocation)
////        fun setUp() {
////            colData = DoubleArray(kRows * 1000) { Math.random() }
////        }
//    }

    @Benchmark
    fun measureMM10() {
        Shipyard()
    }
}