import kravis.SessionPrefs
import kravis.device.SwingPlottingDevice
import org.kalasim.*
import org.kalasim.plot.kravis.display
import kotlin.math.max
import kotlin.math.roundToInt


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

            // complete hangover calls from previous shift
            fun shiftLegacy() = callCenter.claimers.components.filter { it.isInterrupted }

            while(shiftLegacy().isNotEmpty() && callCenter.capacity>0 ){
                val numRunning = callCenter.claimers.components.count { it.isScheduled }
                val spareCapacity = max(0, callCenter.capacity.roundToInt() - numRunning)

                // resume interrupted tasks from last shift to max out new capacity
                shiftLegacy().take(spareCapacity).forEach { it.resume() }

                val shiftLegacy = shiftLegacy()
                log("stalling in while ${numRunning} ${spareCapacity} $shiftLegacy")

                standby()
            }

            // wait for end of shift
            hold(if(currentShift == ShiftID.WeekEnd) 48 else 12)

            // stop and reschedule the ongoing tasks
            callCenter.claimers.components.forEach {
                // detect remaining task time and request this with high prio so that these tasks are picked up next in the upcoming shift
                it.interrupt()
            }
        }
    }

    ComponentGenerator(iat = exponential(0.2)) { Request() }
}

sim.run(600)

SessionPrefs.OUTPUT_DEVICE = SwingPlottingDevice()
val claimedTimeline = sim.get<Resource>().claimers.queueLengthTimeline

sim.get<Resource>().requesters.queueLengthTimeline.display().show()
claimedTimeline.display().show()

// show shift transition
claimedTimeline.display("A-B", from= 11.tt, to = 13.tt).show()
claimedTimeline.display("B-A", from= 23.tt, to = 25.tt).show()
