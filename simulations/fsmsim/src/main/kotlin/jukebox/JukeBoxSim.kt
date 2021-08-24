package jukebox

import JBEvent
import JukeBoxFSM
import org.kalasim.*

fun main() {
    createSimulation {

        val jukeBox = JukeBoxFSM(this)

        class Visitor : Component() {
            override fun process() = sequence {
                // go to the jukebox nd ask for a song
                jukeBox.insertCoin()

                if (jukeBox.isShowingMenu) {
                    hold(1, description = "studying song list")
                    jukeBox.fsm.sendEvent(JBEvent.SelectSong)
                    hold(10, description = "listening")
                }else{
                    hold(1, description = "leaving")
                }

            }
        }

        ComponentGenerator(exponential(5)) { Visitor() }

        // run sim
        run(600)
    }
}