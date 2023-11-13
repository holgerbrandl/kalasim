package org.kalasim.logistics

import java.awt.geom.Point2D
import kotlin.math.*


// define simple geometries
//data class Point(val x: Double, val y: Double)

typealias Point  = Point2D.Double

fun Point.rotate(center: Point, angle: Double): Point {
    val rad = Math.toRadians(angle)
    val cos = cos(rad)
    val sin = sin(rad)
    val x = this.x - center.x
    val y = this.y - center.y
    val newX = x * cos - y * sin + center.x
    val newY = x * sin + y * cos + center.y
    return Point(newX, newY)
}


fun Point.scale(factor: Double): Point = Point(x * factor, y * factor)

fun Point.normalize(): Point {
    val length = sqrt(x * x + y * y)
    return Point(x / length, y / length)
}

data class Rectangle(val offset: Point, val width: Double, val height: Double)


//val someSpeed: Expression = 2.m/s
//val someAcceleration: Expression = 2.m/(s*s)
//someAcceleration.

@JvmInline
value class Speed(val kmh: Number){
    val meterPerSecond : Double
        get()= kmh.toDouble()/3.6

}


val Number.kmh get() = Speed(this.toDouble())
val Number.mph get() = (toDouble()* 1.60934).kmh


data class Distance(val meters: Number)
val Number.meters get() = Distance(toDouble())
val Number.km get() = Distance(1000.0 * toDouble())



