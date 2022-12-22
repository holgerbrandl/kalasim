package org.kalasim.scratch.callcenter.multiresource

import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display


enum class ShiftID { A, B, WeekEnd }

val shiftModel = sequence {
    while(true) {
        repeat(5) { yield(ShiftID.A); yield(ShiftID.B) }
        yield(ShiftID.WeekEnd)
    }
}

// model requests with static duration for now once they got hold of an operator
class Request : Component() {
    val shiftManager = get<ShiftManager>()

    override fun process() = sequence {
        request(shiftManager.currentShift, capacityLimitMode = CapacityLimitMode.SCHEDULE) {
            hold(1)
        }
    }
}

open class ShiftManager : Component() {
    protected val shiftIt = shiftModel.iterator()

    val shiftA = Resource("ShiftA", capacity = 2)
    val shiftB = Resource("ShiftA", capacity = 8)
    val shiftW = Resource("weekend", capacity = 0)

    var currentShift = shiftA


    override fun repeatedProcess() = sequence {
        val nextShiftID = shiftIt.next()

        log("starting new shift $nextShiftID")

        val requests = currentShift.requesters.asSortedList()
        requests.forEach {
            it.component.cancel()
            it.component.activate(process = Request::process)
        }

        currentShift = when(nextShiftID) {
            ShiftID.A -> shiftA
            ShiftID.B -> shiftB
            ShiftID.WeekEnd -> shiftW
        }

        // wait for end of shift
        hold(if(nextShiftID == ShiftID.WeekEnd) 48 else 12)
    }
}


class MultiResourceCallCenter(arrivalRate: Double = 0.3, logEvents: Boolean = true) : Environment(enableConsoleLogger = logEvents) {

    val shiftManager = dependency { ShiftManager() }

    init {
        ComponentGenerator(iat = exponential(arrivalRate)) { Request() }
    }
}

fun main() {
    val sim = MultiResourceCallCenter()

    sim.run(600)

    SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()
    val aRequesters = sim.shiftManager.shiftA.requesters.queueLengthTimeline


    aRequesters.display().show()

// show shift transition
    aRequesters.display("A-B", from = 11.tt, to = 13.tt).show()
    aRequesters.display("B-A", from = 23.tt, to = 25.tt).show()
}