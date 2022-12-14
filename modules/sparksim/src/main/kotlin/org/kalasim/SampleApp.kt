/* SimpleApp.kt */
@file:JvmName("SimpleApp")
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.jetbrains.kotlinx.spark.api.*

import org.jetbrains.kotlinx.spark.api.tuples.*
import org.kalasim.TickTransform
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


fun main() {
    val logFile = "D:\\projects\\scheduling\\kalasim\\modules\\sparksim\\README.md" // Change to your Spark Home path


    //    SparkSession
//        .builder()
//        .config(SparkConf())
//        .master("local[2]")
//        .appName("Simple Application").orCreate

    val conf: SparkConf = SparkConf().setAppName("Java Spark").setMaster("local[*]")

    val sparkSession = SparkSession.builder()
//        .master("spark://hobvm2204:7077")
//        .master("spark://192.168.217.128:7077")
        .config(conf)
        .appName("spark session example")

    withSpark(sparkSession) {
        spark.catalog()
    }

    data class SimConfig(val run:Int)
    withSpark(sparkSession) {
        val arg = List(10) { SimConfig(it) }
        val dd = arg.toDS().map{ it->
//            Math.random()
            org.kalasim.Environment().apply {
                tickTransform = TickTransform(TimeUnit.DAYS)
                run(1.seconds)
            }.currentComponent?.componentState?.name
        }

        dd.show()

    }

    withSpark(sparkSession){
        spark.read().textFile(logFile).withCached {
            val numAs = filter { it.contains("a") }.count()
            val numBs = filter { it.contains("b") }.count()
            println("Lines with a: $numAs, lines with b: $numBs")
        }
    }

    // https://blog.jetbrains.com/kotlin/2020/08/introducing-kotlin-for-apache-spark-preview/
    data class Coordinate(val lon: Double, val lat: Double)
    data class City(val name: String, val coordinate: Coordinate)
    data class CityPopulation(val city: String, val population: Long)


    withSpark(appName = "Find biggest cities to visit") {
        val citiesWithCoordinates = dsOf(
            City("Moscow", Coordinate(37.6155600, 55.7522200)),
            // ...
        )

        val populations = dsOf(
            CityPopulation("Moscow", 11_503_501L),
            // ...
        )

        citiesWithCoordinates
            .rightJoin(populations, citiesWithCoordinates.col("name") `===` populations.col("city"))
            .filter { (_, citiesPopulation) ->
                citiesPopulation.population > 15_000_000L
            }
//            .filter { it ->
//                it._2.population > 15_000_000L
//            }
            .map { it ->
                // A city may potentially be null in this right join!!!
                it._1?.coordinate
            }
            .filterNotNull()
            .show()
    }

}