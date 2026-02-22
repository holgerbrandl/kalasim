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
open class SimpleERBenchmark {

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


@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
open class ExtErBenchmark {

//    var value: Double = 0.0
//
//    @Setup
//    fun setUp(): Unit {
//        value = 3.0
//    }


    @State(Scope.Benchmark)
    open class ExecutionPlan {

        @Param("36","365", "3650")
        var numDays:Int =0

        lateinit var sim: EmergencyRoom

        @Setup(Level.Invocation)
        fun setUp() {
            sim = EmergencyRoom()
        }
    }

    @Benchmark
    fun measureER(execPlan: ExecutionPlan) {
        execPlan.sim.run(execPlan.numDays.days)
    }
}

