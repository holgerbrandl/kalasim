package org.kalasim.scratch.callcenter

import kravis.GGPlot
import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display
import org.kalasim.plot.kravis.showFile
import java.awt.Desktop
import java.awt.Dimension
import java.nio.file.Files
import java.time.Instant
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
//            hold(1.minutes)
            hold(exponential(25.minutes).sample())
        }
    }
}

class ShiftManager : Component() {
    val shiftIt = shiftModel.iterator()
    val callCenter = get<Resource>()

    override fun repeatedProcess() = sequence {
        val currentShift = shiftIt.next()

        log("starting new shift ${currentShift}")

        // adjust shift capacity at the beginning of the shift
        callCenter.capacity = when(currentShift) {
            ShiftID.A -> 2.0
            ShiftID.B -> 5.0
            ShiftID.WeekEnd -> 0.0
        }

        // wait for end of shift
        hold(if(currentShift == ShiftID.WeekEnd) 48.hours else 12.hours)
    }
}

class InterruptingShiftManager(
    val aWorkers: Double = 2.0,
    val bWorkers: Double = 8.0
) : Component() {
    val shiftIt = shiftModel.iterator()
    val serviceAgents = get<Resource>()

    override fun repeatedProcess() = sequence {
        val currentShift = shiftIt.next()

        log("starting new shift $currentShift")

        // adjust shift capacity at the beginning of the shift
        serviceAgents.capacity = when(currentShift) {
            ShiftID.A -> aWorkers
            ShiftID.B -> bWorkers
            ShiftID.WeekEnd -> 0.0
        }

        // complete hangover calls from previous shift
        fun shiftLegacy() = serviceAgents.claimers.components.filter { it.isInterrupted }

        // incrementally resume interrupted tasks while respecting new capacity
        while(shiftLegacy().isNotEmpty() && serviceAgents.capacity > 0) {
            val numRunning = serviceAgents.claimers.components
                .count { it.isScheduled }

            val spareCapacity =
                max(0, serviceAgents.capacity.roundToInt() - numRunning)

            // resume interrupted tasks from last shift to max out new capacity
            shiftLegacy().take(spareCapacity).forEach { it.resume() }

            standby()
        }

        // wait for end of shift
        hold(if(currentShift == ShiftID.WeekEnd) 48.hours else 12.hours)

        // stop and reschedule the ongoing tasks
        serviceAgents.claimers.components.forEach {
            // detect remaining task time and request this with high prio so
            // that these tasks are picked up next in the upcoming shift
            it.interrupt()
        }
    }
}


abstract class CallCenter(
    val interArrivalRate: Duration = 10.minutes,
    logEvents: Boolean = false
) :
    Environment(
        enableComponentLogger = logEvents,
        // note tick duration is just needed here to simplify visualization
        tickDurationUnit = DurationUnit.HOURS
    ) {

    // intentionally not defined at this point
    abstract val shiftManager: Component

    val serviceAgents = dependency { Resource("Service Agents") }

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

    sim.serviceAgents.requesters.queueLengthTimeline
        .display(forceTickAxis = true).showFile()

    val claimedTimeline = sim.serviceAgents.claimers.queueLengthTimeline


    sim.serviceAgents.requesters.queueLengthTimeline
        .display(forceTickAxis = true).showFile()

    claimedTimeline.display(forceTickAxis = true).showFile()

    claimedTimeline.display("A-B", from = 11.tt, to = 13.tt).showFile()
    claimedTimeline.display("B-A", from = 23.tt, to = 25.tt).showFile()

    // now investigate a more correct manager who will interrupt ongoing tasks
    val intSim = object : CallCenter() {
        override val shiftManager = InterruptingShiftManager(2.0)
    }

    intSim.run(30.days)

    intSim.serviceAgents.requesters.queueLengthTimeline
        .display("Request queue length with revised handover process", forceTickAxis = true)
        .showFile()


    // try again but with more customers
    val highWorkloadCenter = object : CallCenter(interArrivalRate = 8.minutes) {
        override val shiftManager = InterruptingShiftManager(2.0)
    }

    highWorkloadCenter.run(30.days)

    highWorkloadCenter.serviceAgents.claimers.queueLengthTimeline.display("high B-A", from = 23.tt, to = 25.tt).showFile()
}
