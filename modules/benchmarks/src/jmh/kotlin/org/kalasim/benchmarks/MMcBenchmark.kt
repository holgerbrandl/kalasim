package org.kalasim.benchmarks

import org.kalasim.examples.MMcQueue
import org.kalasim.misc.AmbiguousDuration
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@OptIn(AmbiguousDuration::class)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
open class MMcBenchmark {

    //    val tsvFile = File("src/jmh/resources/nycflights.tsv.gz")
    @State(Scope.Benchmark)
    open class ExecutionPlan {

        @Param("2", "3", "4")
        var c: Int = 0
    }


    @Benchmark
    fun measureMM10(plan: ExecutionPlan) {
        MMcQueue(c = plan.c, mu = 4, lambda = 12).run(1000)
    }
}