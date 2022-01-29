package org.kalasim.animation

import org.kalasim.*
import org.kalasim.analysis.RescheduledEvent
import java.awt.Point
import java.awt.geom.Point2D
import kotlin.math.sqrt
import kotlin.properties.Delegates

/** A component that has a position on a planar 2D surface. While being on the move, it allows to query its current
 *  position. This is most useful when visualizing a simulation state.
 */
open class AnimationComponent(
    initialPosition: Point2D? = null,
    name: String? = null,
    process: ProcessPointer? = null,
) : Component(name, process = process) {

    private var from: Point2D = initialPosition ?: Point(0, 0)
    private var to: Point2D? = null

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

        hold(Ticks(duration), description ?: "moving to ${nextTarget}", priority)
        from = to!!
        to = null
    }

    private fun distance(): Double {
        val xDist = to!!.x - from.x
        val yDist = to!!.y - from.y

        return sqrt(xDist * xDist + yDist * yDist)
    }

    val currentPosition: Point2D
        get() {
            val currentTo = to // used for better thread safety

            return if(currentTo != null) {
                val percentDone = (now - started) / (estimatedArrival - started)

                val xDist = currentTo.x - from.x
                val yDist = currentTo.y - from.y

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


    private var lastHold = mutableMapOf<String, RescheduledEvent>()

    // note: we could potentially also bypass the event-bus and overload log()
    init {
        env.addEventListener<RescheduledEvent> { re ->
            if(re.component != this) return@addEventListener

            holdTracks
                .filter { (name, matcher) -> matcher(re) }
                .keys.forEach {
                    lastHold[it] = re
                }
        }
    }

    val holdTracks = mutableMapOf<String, AnimationHoldMatcher>()
    fun registerHoldTracker(query: String, eventMatcher: AnimationHoldMatcher) {
        holdTracks[query] = eventMatcher
    }


    fun isHolding(holdId: String): Boolean {
        val rescheduledEvent = lastHold[holdId]

        return rescheduledEvent != null && rescheduledEvent.scheduledFor >= now
    }

    fun holdProgress(holdId: String): Double? {
        val rescheduledEvent = lastHold[holdId]
        if(rescheduledEvent == null || rescheduledEvent.scheduledFor < now) return null

        require(rescheduledEvent.time <= now)

        // calculate fraction
        return (now - rescheduledEvent.time) / (rescheduledEvent.scheduledFor - rescheduledEvent.time)
    }
}

typealias AnimationHoldMatcher = RescheduledEvent.() -> Boolean
