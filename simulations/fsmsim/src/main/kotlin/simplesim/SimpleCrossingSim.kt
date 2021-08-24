package simplesim

import PedTrafficLight
import TrafficLight
import org.kalasim.*
import org.kalasim.plot.kravis.display

fun main() {
    createSimulation {
        val pedRequest = State(false)
        val ped = State(PedTrafficLight.Red)
        val street = State(TrafficLight.Red)

        val crossing = object : Component(){
            override fun process() = sequence {
                while(true){
                    wait(pedRequest, true)

                    street.value = TrafficLight.Yellow
                    hold(5)
                    street.value = TrafficLight.Red
                    hold(2)
                    ped.value = PedTrafficLight.Green
                    pedRequest.value = false
                    hold(15)
                    ped.value = PedTrafficLight.Red
                    hold(3)
                    street.value = TrafficLight.Yellow
                    hold(3)
                    street.value = TrafficLight.Green
                }
            }
        }

        class Pedestrian : Component() {
            override fun process() = sequence {
                if(ped.value!= PedTrafficLight.Green && !pedRequest.value) pedRequest.value = true

                wait(ped, PedTrafficLight.Green)

                hold(10,description= "passing")
            }
        }

        ComponentGenerator(exponential(1)){ Pedestrian() }

        // run sim
        run(600)

        // do some basic analysis
        pedRequest.valueMonitor.display().show()
        ped.valueMonitor.display().show()
        street.valueMonitor.display().show()
    }
}