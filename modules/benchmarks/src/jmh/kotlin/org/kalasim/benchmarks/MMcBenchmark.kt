package org.kalasim.benchmarks

import org.kalasim.examples.MMcQueue
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
open class MMcBenchmark {

//    val tsvFile = File("src/jmh/resources/nycflights.tsv.gz")

    @Benchmark
    fun measureMM10() {
        MMcQueue(c = 10, mu= 4, lambda = 1).run(1000)
    }
}