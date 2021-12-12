import io.jumpco.open.kfsm.StateMachineDefinition
import io.jumpco.open.kfsm.StateMachineInstance
import io.jumpco.open.kfsm.stateMachine
import org.kalasim.Component

enum class JBState { Playing, Menu, Idle }
enum class JBEvent { InsertCoin, SelectSong, SongFinished }

class JukeBox : Component() {
    val jukeBox = JukeBoxFSM(this)

    fun updateFsmIn(delay: Number, callback: JukeBoxFSM.() -> Unit) {
        object : Component() {
            override fun process() = sequence {
                hold(delay)
                callback(jukeBox)
            }
        }
    }

    // internal timers
    override fun process() = sequence<Component> {

    }
}

class JukeBoxFSM(jukeBox: JukeBox) {

    fun insertCoin() {
        fsm.sendEvent(JBEvent.InsertCoin)
    }

    val isShowingMenu: Boolean
        get() = fsm.currentState == JBState.Menu

    val fsm : StateMachineInstance<JBState, JBEvent, JukeBox, Any, Any> = definition.create(jukeBox)

    companion object {
        val definition = stateMachine(JBState.values().toSet(), JBEvent.values().toSet(), JukeBox::class) {
            initialState {
                JBState.Idle
            }

            // define state-machine
            whenState(JBState.Idle) {
                onEvent(JBEvent.InsertCoin to JBState.Menu) {
                    log("showing menu")
                }
            }

            whenState(JBState.Menu) {
                onEvent(JBEvent.SelectSong to JBState.Playing) {
                    // playing song
                    log("playing song")

                    // here we need to make sure that playing stops after 10min
                    updateFsmIn(10) {
                        fsm.sendEvent()
                    }
                }
                onEvent(JBEvent.InsertCoin) {
                    log("returning coin")
                }
            }
        }.build()
    }

}