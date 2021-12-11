package org.kalasim.scratch

import java.util.*


fun main() {

    class DynamicPrioElement(val payload: String, var priority: Int) {
        override fun toString(): String {
            return payload
        }
    }

    val pq = PriorityQueue<DynamicPrioElement> { o1, o2 -> compareValuesBy(o1, o2) { it.priority } }

    val dpeFoo = DynamicPrioElement("foo", 1)
    val dpeBar = DynamicPrioElement("bar", 2)
    val dpeBla = DynamicPrioElement("bla", 3)

    // add them shuffled to ensure that queue has to do the sorting
    pq.add(dpeBar)
    pq.add(dpeBla)
    pq.add(dpeFoo)

    println("num elements in queue ${pq.size}")

    println("first element ${pq.peek()}")

    // adjust prio
    dpeFoo.priority = 4
    println("first element after priority adjustment ${pq.peek()}")

    // does not work!!!

//    https://stackoverflow.com/questions/1871253/updating-java-priorityqueue-when-its-elements-change-priority
    pq.remove(dpeFoo)
    pq.add(dpeFoo)
    println("first element after remove-add ${pq.peek()}")

}