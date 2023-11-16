package org.kalasim.logistics

import org.kalasim.animation.AnimationComponent
import java.awt.geom.Point2D


open class Vehicle(startingPosition: Port, val speed: Speed = 100.kmh) :
    AnimationComponent(startingPosition.position) {

    /** The last visited port. When not in motion the last visited port.*/
    var currentPort: Port = startingPosition
    val pathFinder = get<PathFinder>()

     fun moveTo(target: Port) = sequence {
        val path = pathFinder.findPath(currentPort, target)

        path.route.vertexList.forEach { node ->
            val nextTarget: Point2D = Point2D.Double(node.position.x, node.position.y)
            move(nextTarget, speed = speed.kmh.toDouble(), description = "moving ${this@Vehicle} to $nextTarget")
        }
    }

    fun move(target: Port){
        activate(Vehicle::moveTo, target)
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