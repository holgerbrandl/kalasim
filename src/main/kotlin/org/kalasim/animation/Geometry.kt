package org.kalasim.animation

class Speed(val kmh: Number) {
    val meterPerSecond: Double
        get() = kmh.toDouble() / 3.6

}

class Acceleration(val metersPerSecondSquared: Number)


val Number.acc get() = Acceleration(toDouble())
val Number.kmh get() = Speed(this.toDouble())
val Number.mph get() = (toDouble() * 1.60934).kmh


data class Distance(val meters: Number)

val Number.meters get() = Distance(toDouble())
val Number.km get() = Distance(1000.0 * toDouble())
