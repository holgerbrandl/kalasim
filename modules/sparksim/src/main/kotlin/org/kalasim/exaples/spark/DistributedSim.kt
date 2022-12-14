/* SimpleApp.kt */
@file:JvmName("DistributedSim")
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.jetbrains.kotlinx.spark.api.*

import org.jetbrains.kotlinx.spark.api.tuples.*
import org.kalasim.Environment
import org.kalasim.TickTransform
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


fun main() {
//    val conf: SparkConf = SparkConf().setAppName("Java Spark").setMaster("local[*]")

    val sparkSession = SparkSession.builder()
        .master("spark://hobvm2204:7077")
//        .master("spark://192.168.217.128:7077")
//        .config(conf)
        .appName("spark_kalasim_example")


    data class SimConfig(val run:Int)

    withSpark(sparkSession) {
        val arg = List(10) { SimConfig(it) }
        val dd = arg.toDS().map{ it->
//            Math.random()
            val result = Environment(randomSeed = it.run).apply {
                tickTransform = TickTransform(TimeUnit.DAYS)
                run(1.seconds)
            }.currentComponent?.componentState?.name  ?: "no_result"

            result
        }

        dd.show()

    }

}