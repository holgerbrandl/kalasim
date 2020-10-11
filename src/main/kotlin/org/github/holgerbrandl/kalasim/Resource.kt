package org.github.holgerbrandl.kalasim;

import java.util.*

class Resource(name: String, val capacity: Int, val isPreemptive: Boolean = false, val anonymous: Boolean=false) : Component(name = name, process = null) {

    fun availableQuantity(): Int {
        TODO()

    }

    fun release(quantity: Int) {
        TODO("Not yet implemented")
    }

    val requesters = PriorityQueue<Component>()
    val claimers = PriorityQueue<Component>()
}


