package org.github.holgerbrandl.basamil

import java.util.*

class Environment(trace: Boolean = true) {

    private val components: MutableList<Component> = listOf<Component>().toMutableList()

    val main = Component(env = this, name = "main")

    private var running: Boolean = false
    private var stopped: Boolean = false

    private var curComponent: Component? = main

     val eventQueue = PriorityQueue<QueueElement>()

    fun build(builder: (Environment.() -> Environment)): Environment {
        builder(this)
        return (this);
    }

    fun addComponent(c: Component) = components.add(c)


    /**
     *         start execution of the simulation
     */
    fun run(until: Int = Int.MAX_VALUE) {
        val scheduled_time = until

//        main.reschedule(scheduled_time, priority, urgent, "run", extra=extra)
        main.reschedule(scheduled_time)

        running = true
        while (running) {
            step()
        }
    }

    private fun step() {
        TODO("Not yet implemented")
    }

    fun now(): Int = main.now()

    /**
     *         prints a trace line
     *
     *   @param s1 (usually formatted  now), padded to 10 characters
     *  @param s2 (usually only used for the compoent that gets current), padded to 20 characters
     */
    fun printTrace(s: String, s1: String, s2: String, s3: String) {
        println(s + s2 + s3)
    }


}