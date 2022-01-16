package org.kalasim.scratch.callcenter.experiments

import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display


enum class ShiftID { A, B, WeekEnd }


val sim = createSimulation(true) {

    val shiftModel = sequence {
        while(true) {
            repeat(5) { yield(ShiftID.A); yield(ShiftID.B) }
            yield(ShiftID.WeekEnd)
        }
    }.iterator()

    val callCenter = dependency { Resource("Call Center") }

    // model requests with static duration for now once they got hold of an operator
    class Request : Component() {
        override fun process() = sequence {
            request(callCenter, capacityLimitMode = CapacityLimitMode.SCHEDULE) {
                hold(1)
            }
        }
    }

    object : Component("Business Process") {
        override fun repeatedProcess() = sequence {
            val currentShift = shiftModel.next()

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

    ComponentGenerator(iat = exponential(0.2)) { Request() }
}

sim.run(600)

SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()
sim.get<Resource>().requesters.queueLengthTimeline.display().show()
