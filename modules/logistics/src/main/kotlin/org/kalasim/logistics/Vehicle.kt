package org.kalasim.logistics

import org.kalasim.animation.AnimationComponent
import java.awt.geom.Point2D


open class Vehicle(startingPosition: Port, val speed: Speed = 100.kmh, val acc: Acceleration = 2.acc) :
    AnimationComponent(startingPosition.position) {

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port = startingPosition
    val pathFinder = get<PathFinder>()

    fun moveTo(target: Port) = sequence {
//        logger.info{ "computing port from $currentPort to $target"}
        val path = pathFinder.findPath(currentPort, target)

        logger.info { "computed port from $ to $target" }

        path.route.vertexList.forEach { node ->
            val nextTarget: Point2D = Point2D.Double(node.position.x, node.position.y)
            move(nextTarget, speed = speed.meterPerSecond, description = "moving ${this@Vehicle} to $nextTarget")
        }
    }
}

//fun main() {
//
//    suspend fun foo() : Sequence<Component> = sequence { yield(Component())}
//    suspend fun bar(smthg:String)  : Sequence<Component> = sequence { yield(Component()) }
//
//
//    val fun1 : KFunction1<*, String> = ::foo
//    val fun2 : KSuspendFunction1<*,  String> = ::foo
//    val fun3 : KSuspendFunction1<*, *, String> = ::foo
//    val fun4 : KSuspendFunction2<*, *, String> = ::bar
//
//}