package org.kalasim.logistics

import org.kalasim.animation.Point
import kotlin.math.*


// define simple geometries
//data class Point(val x: Double, val y: Double)


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

typealias Rectangle = java.awt.geom.Rectangle2D.Double
//data class Rectangle(val offset: Point, val width: Double, val height: Double){
//    constructor(upperLeft:Point, lowerRight:Point): this(upperLeft, lowerRight.x-upperLeft.x, lowerRight.y-upperLeft.y )
//}

fun Rectangle(upperLeft: Point, lowerRight: Point) =
    Rectangle(upperLeft.x, upperLeft.y, lowerRight.x - upperLeft.x, lowerRight.y - upperLeft.y)


//val someSpeed: Expression = 2.m/s
//val someAcceleration: Expression = 2.m/(s*s)
//someAcceleration.

//todo inline blocked by https://github.com/Kotlin/dataframe/issues/526
//@JvmInline
//value



