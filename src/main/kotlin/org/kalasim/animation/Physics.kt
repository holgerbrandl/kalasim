package org.kalasim.animation

import java.awt.geom.Point2D
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class Speed internal constructor(val metersPerSecond: Number) {
    val kmh: Double
        get() = metersPerSecond.toDouble() * 3.6
}

val Number.metersPerSecond get() = Speed(this.toDouble())
val Number.kmh get() = Speed(this.toDouble() / 3.6)
val Number.mph get() = (toDouble() * 1.60934).kmh


class Acceleration(val metersPerSecondSquared: Number)

val Number.acc get() = Acceleration(toDouble())


data class Distance(val meters: Double) : Comparable<Distance> {
    constructor(meters: Number) : this(meters.toDouble())

    operator fun div(speed: Speed): Duration = (meters / speed.metersPerSecond.toDouble()).seconds

    operator fun times(scaling: Double): Distance = Distance(this.meters * scaling)
    operator fun plus(other: Distance) = Distance(this.meters + other.meters)
    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)
}

val Number.meters get() = Distance(toDouble())
val Number.km get() = Distance(1000.0 * toDouble())


/** A point in the metric system. That is the distance between Point(0,0) and Point(0,1) is 1.meter */
typealias Point = Point2D.Double

/** A point in the metric system. That is the distance between Point(0,0) and Point(0,1) is 1.meter */
// secondary constructor which needs to be modeled as function because kotlin does not allow otherwise
@Suppress("unused")
fun Point(x: Number, y: Number): Point = Point(x.toDouble(), y.toDouble())

operator fun Point.minus(from: Point): Distance {
    val xDist = x - from.x
    val yDist = y - from.y

    return sqrt(xDist * xDist + yDist * yDist).meters
}