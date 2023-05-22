import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    kotlin("jvm") version "1.8.20"
    id("java")
    id("me.champeau.jmh") version "0.7.1"
//    id("io.morethan.jmhreport") version "0.9.0"

}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
//    jmh("org.openjdk.jmh:jmh-core:0.9")
//    jmh( "org.openjdk.jmh:jmh-generator-annprocess:0.9")

//    jmh(kotlin("stdlib"))
    jmh("org.apache.commons:commons-lang3:3.12.0")
    jmh("org.slf4j:slf4j-simple:1.6.1")

//    api("com.github.holgerbrandl:kalasim:0.9.2-SNAPSHOT")
    jmh("com.github.holgerbrandl:kalasim:0.9.2-SNAPSHOT")
//    jmh(project(":"))
}


val commitHash = Runtime
    .getRuntime()
    .exec("git rev-parse --short HEAD")
    .let { process ->
        process.waitFor()
        val output = process.inputStream.use {
            it.bufferedReader().use(BufferedReader::readText)
        }
        process.destroy()
        output.trim()
    }

val timestamp: String = SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date())
//fun getDate(): String = Date().toString()//.format("yyyyMMdd_HHmm")

jmh {
    // Configure the JMH task here
//        include = "org\\.example\\.benchmark.*"
//        resultFormat = "csv"
//        resultsFile = file('perf_logs/jmh_results_' + getDate()+ '.csv')
    //        profilers = listOf("gc", "stack", "hs_thr")

    // Additional configurations for the task (if needed)
//    jmhTask.group = "benchmark"
//    description.set("Runs JMH benchmarks")

//val jmh by tasks.getting(JmhPluginExtension::class) {
//jmh {
//    jmhVersion = "1.34"
//    jvmArgs = listOf("-Xms2048m", "-Xmx2048m")

//    include = 'org\\..*Bench.*'
    resultFormat.set("csv")
//    resultFormat = "csv"
//    resultFormat = 'csv'
    resultsFile.set(File("perf_logs/benchmarks.json"))
    resultsFile.set(File("perf_logs/jmh_results_${timestamp}_${commitHash}.csv"))
//    resultsFile = file('perf_logs/jmh_results_' + getDate()+ '.csv')

    // for list of available profilers see http://java-performance.info/introduction-jmh-profilers/
//    profilers = Arrays.asList("")
//}
}


