package org.kalasim.benchmarks

import org.kalasim.Environment
import org.kalasim.examples.elevator.Elevator
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@State(Scope.Benchmark)
//@Fork(4)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
open class ElevatorBenchmark {

    @State(Scope.Benchmark)
    open class ExecutionPlan {

        @Param("10", "20","30")
        var numFloors: Int = 0

//        lateinit var colData: DoubleArray
//        @Setup(Level.Invocation)
//        fun setUp() {
//            colData = DoubleArray(kRows * 1000) { Math.random() }
//        }
    }


    @Benchmark
//    @Fork(value = 1, warmups = 1)
//    @Warmup(iterations = 2)
//    @Measurement(iterations = 5)
    fun measureElevator(plan: ExecutionPlan) {
        // auto-scale not just the floors but also the other parameters of the problem
        val env = Elevator(
            topFloor = plan.numFloors,
            numCars = plan.numFloors / 10,
            load0N = plan.numFloors*5,
            loadN0 = plan.numFloors*8,
            loadNN = plan.numFloors*8
        )

        env.run(30.days)
    }
}

// does not work without jar deployment, see http://openjdk.java.net/projects/code-tools/jmh/
//fun main(args: Array<String>) {
//    org.openjdk.jmh.Main.main(args);
//}
