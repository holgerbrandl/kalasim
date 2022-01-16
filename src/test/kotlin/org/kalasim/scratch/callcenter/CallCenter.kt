package org.kalasim.scratch.callcenter

import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display
import kotlin.math.max
import kotlin.math.roundToInt


enum class ShiftID { A, B, WeekEnd }

val shiftModel = sequence {
    while(true) {
        repeat(5) { yield(ShiftID.A); yield(ShiftID.B) }
        yield(ShiftID.WeekEnd)
    }
}


// model requests with static duration for now once they got hold of an operator
class Request : Component() {
    val callCenter = get<Resource>()

    override fun process() = sequence {
        request(callCenter, capacityLimitMode = CapacityLimitMode.SCHEDULE) {
            hold(1)
        }
    }
}

open class ShiftManager : Component() {
    val shiftIt = shiftModel.iterator()
    val callCenter = get<Resource>()

    override fun repeatedProcess() = sequence {
        val currentShift = shiftIt.next()

        log("starting new shift ${currentShift}")

        // adjust shift capacity at the beginning of the shift
        callCenter.capacity = when(currentShift) {
            ShiftID.A -> 2.0
            ShiftID.B -> 8.0
            ShiftID.WeekEnd -> 0.0
        }

        // wait for end of shift
        hold(if(currentShift == ShiftID.WeekEnd) 48 else 12)
    }
}

class InterruptingShiftManager: ShiftManager() {
    override fun repeatedProcess() = sequence {
        val currentShift = shiftIt.next()

        log("starting new shift $currentShift")

        // adjust shift capacity at the beginning of the shift
        callCenter.capacity = when(currentShift) {
            ShiftID.A -> 2.0
            ShiftID.B -> 8.0
            ShiftID.WeekEnd -> 0.0
        }

        // complete hangover calls from previous shift
        fun shiftLegacy() = callCenter.claimers.components.filter { it.isInterrupted }

        // incrementally resume interrupted tasks while respecting new capacity
        while(shiftLegacy().isNotEmpty() && callCenter.capacity > 0) {
            val numRunning = callCenter.claimers.components.count { it.isScheduled }
            val spareCapacity = max(0, callCenter.capacity.roundToInt() - numRunning)

            // resume interrupted tasks from last shift to max out new capacity
            shiftLegacy().take(spareCapacity).forEach { it.resume() }

            standby()
        }

        // wait for end of shift
        hold(if(currentShift == ShiftID.WeekEnd) 48 else 12)

        // stop and reschedule the ongoing tasks
        callCenter.claimers.components.forEach {
            // detect remaining task time and request this with high prio so
            // that these tasks are picked up next in the upcoming shift
            it.interrupt()
        }
    }
}


abstract class CallCenter(val arrivalRate: Double = 0.3, logEvents: Boolean = true) : Environment(logEvents) {
    // not defined at this point
    abstract val shiftManager : ShiftManager

    val callCenter = dependency { Resource("Call Center") }

    init{
        ComponentGenerator(iat = exponential(arrivalRate)){ Request() }
    }
}


fun main() {
    val sim = object: CallCenter() {
        override val shiftManager = ShiftManager()
    }

    sim.run(600)

    sim.callCenter.requesters.queueLengthTimeline.display()

    SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()
    val claimedTimeline = sim.callCenter.claimers.queueLengthTimeline


    sim.callCenter.requesters.queueLengthTimeline.display().show()
    claimedTimeline.display().show()

    claimedTimeline.display("A-B", from = 11.tt, to = 13.tt).show()
    claimedTimeline.display("B-A", from = 23.tt, to = 25.tt).show()


    // now investigate a more correct manager who will interrupt ongoing tasks
    val intSim = object: CallCenter() {
        override val shiftManager = InterruptingShiftManager()
    }

    intSim.run(600)


    // try again but with more customers
    val highWorkLoadSim = object: CallCenter(arrivalRate = 0.2) {
        override val shiftManager = InterruptingShiftManager()
    }
    highWorkLoadSim.run(600)

    highWorkLoadSim.callCenter.claimers.queueLengthTimeline.display("high B-A", from = 23.tt, to = 25.tt).show()
}