package org.kalasim.animation

import org.kalasim.misc.roundAny
import java.awt.geom.Point2D
import kotlin.math.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


/**
 * Type-safe wrapper around a speed measurement.
 */
class Speed internal constructor(val metersPerSecond: Number) {
    operator fun minus(other: Speed) =
        Speed(metersPerSecond.toDouble() - other.metersPerSecond.toDouble())

    operator fun times(duration: Duration): Distance =
        Distance(metersPerSecond.toDouble() * duration.toDouble(DurationUnit.SECONDS))

    val kmh: Double
        get() = metersPerSecond.toDouble() * 3.6

    override fun toString() = "${metersPerSecond.toDouble().roundAny(2)} m/s"
    operator fun compareTo(other: Speed) = metersPerSecond.toDouble().compareTo(other.metersPerSecond.toDouble())
}

val Number.metersPerSecond get() = Speed(this.toDouble())
val Number.kmh get() = Speed(this.toDouble() / 3.6)
val Number.mph get() = (toDouble() * 1.60934).kmh


class Acceleration(val metersPerSecondSquared: Number)

val Number.acc get() = Acceleration(toDouble())


data class Distance(val meters: Double) : Comparable<Distance> {
    constructor(meters: Number) : this(meters.toDouble())

    operator fun div(speed: Speed): Duration = (meters / speed.metersPerSecond.toDouble()).seconds

    operator fun div(other: Distance): Double = meters / other.meters

    operator fun times(scaling: Double): Distance = Distance(this.meters * scaling)
    operator fun plus(other: Distance) = Distance(this.meters + other.meters)
    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)
    operator fun unaryMinus(): Distance = Distance(-meters)

    val absoluteValue: Distance
        get() = if(meters <= 0.0) meters.absoluteValue.meters else this
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

fun Point.distanceTo(from: Point): Distance {
    val xDist = x - from.x
    val yDist = y - from.y

    return sqrt(xDist * xDist + yDist * yDist).meters
}


fun List<Point>.hasCollision(dist: Distance = 1.meters) =
    any { point -> any { it != point && it.distance(point) < dist.meters } }


fun List<Point>.minimalDistance(): Double {
    return if(size < 2) 0.0 else withIndex().flatMap { (i, a) ->
        drop(i + 1).map { b -> sqrt((a.x - b.x).pow(2.0) + (a.y - b.y).pow(2.0)) }
    }.minOrNull() ?: Double.MAX_VALUE
}