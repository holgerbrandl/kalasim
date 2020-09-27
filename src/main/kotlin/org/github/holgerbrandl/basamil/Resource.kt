package org.github.holgerbrandl.basamil;

import java.util.*

public class Resource(name: String, env: Environment, val isPreemptive: Boolean = false) :
    Component(env = env, name = name, process = null) {
    fun availableQuantity():Int {
        TODO()

    }

    val requesters = PriorityQueue<Component>()
    val claimers = PriorityQueue<Component>()
}
