package org.github.holgerbrandl.desim;

import java.util.*

class Resource(name: String, val isPreemptive: Boolean = false) : Component(name = name, process = null) {

    fun availableQuantity(): Int {
        TODO()

    }

    fun release(quantity: Int) {
        TODO("Not yet implemented")
    }

    val requesters = PriorityQueue<Component>()
    val claimers = PriorityQueue<Component>()
}
