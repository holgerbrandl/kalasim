package org.kalasim.benchmarks

import org.kalasim.Environment
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

@State(Scope.Benchmark)
@Fork(4)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
open class BackendBench {

    @State(Scope.Benchmark)
    open class ExecutionPlan {

        @Param("1", "10", "100", ) // todo go up to 1E5 = 100M rows
        internal var kRows: Int = 0

        //        @Param({"foo", "bar"})
        //        String something;

        lateinit var colData: DoubleArray

        @Setup(Level.Invocation)
        fun setUp() {
            colData = DoubleArray(kRows * 1000) { Math.random() }
        }
    }


    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(value = 1, warmups = 1)
    @Measurement(iterations = 5)
    //    @BenchmarkMode(Mode.Throughput)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(iterations = 0)
    fun columnArithmetics(plan: ExecutionPlan) {
        val env = Environment()
        env.run(10.days)
    }
}

// does not work without jar deployment, see http://openjdk.java.net/projects/code-tools/jmh/
//fun main(args: Array<String>) {
//    org.openjdk.jmh.Main.main(args);
//}
