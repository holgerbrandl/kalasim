package org.kalasim.test

import org.kalasim.Component
import org.kalasim.createSimulation

createSimulation {

    object : Component(){
        override fun process(): Sequence<Component> {
            return super.process()
        }
    }
}