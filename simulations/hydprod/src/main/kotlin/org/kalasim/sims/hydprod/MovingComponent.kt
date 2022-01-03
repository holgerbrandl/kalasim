package org.kalasim.sims.hydprod

import org.kalasim.*
import java.awt.geom.Point2D
import kotlin.math.sqrt
import kotlin.properties.Delegates


//TODO Add route support, so that entity moves alon planned route

/** A component that has a position on a planar 2D surface. While being on the move, it allows to query its current
 *  position. This is most useful when visualizing a simulation state.
 */
open class MovingComponent(
    initialPosition: Point2D,
    name: String? = null,
    process: ProcessPointer? = null,
) : Component(name, process = process) {

    private var from: Point2D = initialPosition
    var to: Point2D? = null

    private var started by Delegates.notNull<TickTime>()
    private var currentSpeed by Delegates.notNull<Double>()
    private lateinit var estimatedArrival: TickTime

    suspend fun SequenceScope<Component>.move(
        nextTarget: Point2D,
        speed: Double,
        description: String? = null,
        priority: Priority = Priority.NORMAL,
    ) {
        to = nextTarget
        started = now
        currentSpeed = speed

        val distance = distance()
        val duration = distance / speed
        estimatedArrival = now + duration

        hold(Ticks(duration), description ?:"moving to ${nextTarget}", priority)
        to = null
    }

    private fun distance(): Double {
        val xDist = to!!.x - from.x
        val yDist = to!!.y - from.y

        return sqrt(xDist * xDist + yDist * yDist)
    }

    val currentPosition: Point2D
        get() {
            return if(to != null) {
                val percentDone = (now - started) / (estimatedArrival - started)

                val xDist = to!!.x - from.x
                val yDist = to!!.y - from.y

                Point2D.Double(
//                    from.x*(1-percentDone) + to!!.x*percentDone,
//                    from.y*(1-percentDone) + to!!.y*percentDone
                    from.x + percentDone * xDist, from.y + percentDone * yDist
                )

            } else {
                from
            }
        }

//    val gridPosition
//        get() = GridPosition(position.x.roundToInt(), position.y.roundToInt())

//    fun SequenceScope<Component>.move(to: GridPosition) = sequence {
//        val route = planRoute(position, Point2D.Double(to.x.toDouble(), to.y.toDouble()))
//
//        route.forEach {
//            hold(1)
//            position = it
//        }
//    }
//
//    fun SequenceScope<Component>.planRoute(from: Point2D, to: Point2D, distancePerTick: Number = 0.1) = sequence {
//        val xDist = to.x - from.x
//        val yDist = to.y - from.y
//
//        val distance = sqrt(xDist * xDist + yDist * yDist)
//        val numMoves = distance / distancePerTick.toDouble()
//
//        val xInc = xDist / numMoves
//        val yInc = yDist / numMoves
//
//        // todo this seems slightly wrong
//        for(i in 0 until floor(numMoves).toInt()) {
//            yield(Point2D.Double(from.x + i * xInc, from.y + i * yInc))
//
//            // to avoid any rounding issues
//            yield(to)
//        }
//    }
}