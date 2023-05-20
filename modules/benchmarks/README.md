# Benchmarks

Objective
> A benchmark suite to track kalasim performance over time, and to enable library optimization

For gradle plugin options see https://github.com/melix/jmh-gradle-plugin


### How to run?

* This will execute all (methods annotated with `@Benchmark`) benchmarks with their predefined parameters:

`./gradlew --console=plain clean jmh`

* Output is saved as CSV in `benchmarks/build/results/jmh/results.csv`

### How to build report


### How is repo history preserved?

### Run directly

A bit clunky but gives more control over parameters and what is actually getting executed 

* Display command line options:
```
cd benchmarks$
java -jar build/libs/benchmarks-jmh.jar -h`
```

* Run specific benchmark(s) with specific parameters: 
```
cd benchmarks
java -jar build/libs/benchmarks-jmh.jar -wi 2 -i 2 -f 1 -tu ms -bm avgt CompressedTsvBenchmarks
```