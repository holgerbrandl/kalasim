@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.json.JSONObject
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.misc.Jsonable
import java.util.logging.Level


/** The base event of kalasim. Usually this extended to convey more specific information.*/
abstract class Event(
    val time: TickTime
) : Jsonable() {

//    constructor(time: TickTime) : this(time.value)
//    constructor(time: Number) : this(time.toDouble())

    open val logLevel: Level get() = Level.INFO

    override fun toString() = toJson().toString()


    override fun toJson(): JSONObject = json {
        "time" to time
        "type" to this@Event.javaClass.simpleName
    }
}


fun interface EventListener {
    fun consume(event: Event)
}


/**
 * Collects all events on the kalasim event bus. See [Event Log](https://www.kalasim.org/events/) for details.
 *
 * @sample org.kalasim.dokka.eventsHowTo
 */
//@Deprecated("Use ", replaceWith = ReplaceWith("collect<Event>()"))
fun Environment.eventLog(): EventLog {
    val tc = dependency { EventLog() }
    addEventListener(tc)

    return tc
}

/** A list of all events that were created in a simulation run.  See [Event Log](https://www.kalasim.org/events/) for details. */
class EventLog(val events: MutableList<Event> = mutableListOf()) : EventListener,
    MutableList<Event> by events {
//    val events = mutableListOf<Event>()

    override fun consume(event: Event) {
        events.add(event)
    }

    operator fun invoke() = events
//    operator fun get(index: Int): Event = events[index]
}


/**
 * Subscribe to events of a certain type and return a reference to a list into which these events are deposited.
 * See [Event Log](https://www.kalasim.org/events/) for details.
 *
 * @sample org.kalasim.dokka.eventsHowTo
 */
inline fun <reified E : Event> Environment.collect(): List<E> {
    val traces: MutableList<E> = mutableListOf()

    addEventListener {
        if(it is E) traces.add(it)
    }

    return traces
}

/**
 * Collects all components created in the parent environment. See [Event Log](https://www.kalasim.org/events/) for details.
 */
fun Environment.componentCollector(): List<Component> {
    val components: MutableList<Component> = mutableListOf()

    addEventListener {
        if(it is EntityCreatedEvent && it.entity is Component) components.add(it.entity)
    }

    return components
}
