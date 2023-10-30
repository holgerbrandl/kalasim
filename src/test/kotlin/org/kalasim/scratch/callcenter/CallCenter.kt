package org.kalasim.scratch.callcenter

import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


enum class ShiftID { A, B, WeekEnd }

val shiftModel = sequence {
    while(true) {
        repeat(5) { yield(ShiftID.A); yield(ShiftID.B) }
        yield(ShiftID.WeekEnd)
    }
}


class Request : Component() {
    val callCenter = get<Resource>()

    override fun process() = sequence {
        request(callCenter, capacityLimitMode = CapacityLimitMode.SCHEDULE) {
            // model requests with static duration for now once they got hold of an operator
            hold(1.minutes)
//            hold(exponential(1.minutes).sample())
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
        hold(if(currentShift == ShiftID.WeekEnd) 48.hours else 12.hours)
    }
}

class InterruptingShiftManager : ShiftManager() {
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
        hold(if(currentShift == ShiftID.WeekEnd) 48.hours else 12.hours)

        // stop and reschedule the ongoing tasks
        callCenter.claimers.components.forEach {
            // detect remaining task time and request this with high prio so
            // that these tasks are picked up next in the upcoming shift
            it.interrupt()
        }
    }
}


abstract class CallCenter(val interArrivalRate: Duration = 18.minutes, logEvents: Boolean = false) :
    Environment(
        enableComponentLogger = logEvents,
        // note tick duration is just needed here to simplify visualization
        tickDurationUnit = DurationUnit.HOURS
    ) {

    // intentionally not defined at this point
    abstract val shiftManager: ShiftManager

    val callCenter = dependency { Resource("Call Center") }

    init {
        ComponentGenerator(iat = exponential(interArrivalRate)) { Request() }
    }
}


fun main() {
    val sim = object : CallCenter() {
        override val shiftManager = ShiftManager()
    }

    sim.run(30.days)

    SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice() // bug in library, kernel detection seems buggy

    sim.callCenter.requesters.queueLengthTimeline.display(forceTickAxis = true)//.show()

    val claimedTimeline = sim.callCenter.claimers.queueLengthTimeline


    sim.callCenter.requesters.queueLengthTimeline.display(forceTickAxis = true).save(File("tt.png").toPath())//show()
    claimedTimeline.display(forceTickAxis = true).show()

        claimedTimeline.display("A-B", from = 11.tt, to = 13.tt).show()
    claimedTimeline.display("B-A", from = 23.tt, to = 25.tt).show()
//    with(sim){
//        claimedTimeline.display("A-B", forceTickAxis = true, from = 11.asSimTime(), to = 13.asSimTime()).show()
//        claimedTimeline.display("B-A", forceTickAxis = true,  from = 23.asSimTime(), to = 25.asSimTime()).show()
//    }

    // now investigate a more correct manager who will interrupt ongoing tasks
    val intSim = object : CallCenter() {
        override val shiftManager = InterruptingShiftManager()
    }

    intSim.run(30.days)

    intSim.callCenter.requesters.queueLengthTimeline.display("Request queue length with revised handover process", forceTickAxis = true)
        .show()

    // try again but with more customers
    val highWorkloadCenter = object : CallCenter(interArrivalRate = 12.minutes) {
        override val shiftManager = InterruptingShiftManager()
    }

    highWorkloadCenter.run(30.days)

    highWorkloadCenter.callCenter.claimers.queueLengthTimeline.display("high B-A", from = 23.tt, to = 25.tt).show()
}