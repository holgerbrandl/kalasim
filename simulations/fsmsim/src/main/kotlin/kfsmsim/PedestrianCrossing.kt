import CrossingEvents.*
import io.jumpco.open.kfsm.async.asyncStateMachine
import org.kalasim.Component
import org.kalasim.State
import org.kalasim.createSimulation

enum class CrossingEvents {
    PedRequest, TogglePedGreen, ToggleStreetGreen
}

enum class CrossingStates {
    // State: Off
    Off, YellowOn, YellowOff,

    // PedWait
    PedWaitingOn, PedWaitingOff,
    StreetAttention,

    StreetRed, PedGreen, PedRed, StreetPrepare, StreetGreen, Safe
}

//class PedCrossingFSM {
//
//}
class PedCrossing : Component() {

    val pedRequest = State(false)
    val ped = State(PedTrafficLight.Red)


    val pedTL = PedTrafficLight.Red
    val trafficLight = TrafficLight.Red

    val fsm = definition.create(this)

    fun pedRequest() {
        fsm.sendEvent(PedRequest)
    }

    fun enablePedCrossing() {
        fsm.sendEvent(TogglePedGreen)
    }

    fun disablePedCrossing() {
        fsm.sendEvent(ToggleStreetGreen)
    }

    override fun process() = sequence<Component> {


        hol23()


        val fsm = asyncStateMachine(CrossingStates.values().toSet(), values().toSet(), PedCrossing::class) {

            suspend fun SequenceScope<Component>.holdSim(time: Number, callback: () -> Unit) {
//                with(this@PedCrossing) {
//                    with(this@PedCrossing) {
                hold(time)
//                    }
                callback()
//                object : Component() {
//                    override fun process() = sequence {
//                        hold(time)
//                        callback()
//                    }
//                }
            }

            // test if  this construct compiles
            holdSim(1) {}

            initialState {
                CrossingStates.Safe
            }


            // define state-machine
            whenState(CrossingStates.StreetGreen) {
                onEntry(action = { crossingStates, crossingStates2, any ->
                    hold(2)
                })
                onEvent(PedRequest) {
//                    with(this@PedCrossing)
//                    {
//                       hold(2)
//                    }
                    hol23()
                    this@sequence.hold(23)
//                    holdSim(2) { enablePedCrossing() }
                }
            }

            whenState(CrossingStates.StreetGreen) {
//                timeout(CrossingStates.StreetGreen, 1) {
//                    hold(1)
//                }
                onEvent(PedRequest) {
                    holdSim(2) { enablePedCrossing() }
//                        hold()
                }
            }

            whenState(CrossingStates.Safe) {

            }
//            stateMap("off", setOf()){
//            }


        }.build().create(PedCrossing())
    }

    private suspend fun SequenceScope<Component>.hol23() {
        this.hold(23)
    }

    companion object {
        // adopted from https://www.hackster.io/robin-herrmann/traffic-lights-using-finite-state-machine-in-c-for-arduino-26169c
        val definition =
    }
}

fun main() {
    createSimulation() {
        val pedCrossing = PedCrossing()

        pedCrossing.fsm.sendEvent(PedRequest)


        run(5)
    }
}

enum class PedTrafficLight { Red, Green }
enum class TrafficLight { Red, Yellow, Green }