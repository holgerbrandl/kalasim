package org.kalasim.benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1)
@Warmup(iterations = 3)
@Measurement(iterations = 2)
open class CompressedTsvBenchmarks {

//    val tsvFile = File("src/jmh/resources/nycflights.tsv.gz")

    @Benchmark
    fun readTsv(bh: Blackhole) {
        Thread.sleep(2.seconds.inWholeMilliseconds)
//        val df = DataFrame.readTSV(tsvFile).apply {
//            check(nrow == 336776)
//            check(ncol == 16)
//        }
//        bh.consume(df)
    }
}