package org.github.holgerbrandl.desimuk;

import java.util.*

public class Resource(name: String, env: Environment, val isPreemptive: Boolean = false) :
    Component(env = env, name = name, process = null) {
    fun availableQuantity():Int {
        TODO()

    }

    fun release(quantity: Int) {
        TODO("Not yet implemented")
    }

    val requesters = PriorityQueue<Component>()
    val claimers = PriorityQueue<Component>()
}
