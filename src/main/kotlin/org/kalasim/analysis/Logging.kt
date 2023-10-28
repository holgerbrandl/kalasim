@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import com.github.holgerbrandl.jsonbuilder.json
import org.json.JSONObject
import org.kalasim.analysis.EntityCreatedEvent
import org.kalasim.misc.Jsonable

fun main() {
    val name = getNameClassValueCache(Event::class.java)
    print("name is $name")
}

fun getNameClassValueCache(clazz: Class<*>): String {
    val default = ClassName.DEFAULT
    return default.get(clazz)
}


internal class ClassName : ClassValue<String>() {
    override fun computeValue(clazz: Class<*>): String {
        val name = clazz.getName()
        return name.substring(name.lastIndexOf('.') + 1)
    }

    companion object {
        val DEFAULT: ClassValue<String> = ClassName()
    }
}

/** The base event of kalasim. Usually this extended to convey more specific information.*/
abstract class Event(
    val time: TickTime,
) : Jsonable() {

//    constructor(time: TickTime) : this(time.value)
//    constructor(time: Number) : this(time.toDouble())

    // disabled because was just used in component-logger. It should better use tracking-policy instead
//    open val logLevel: Level get() = Level.INFO

    //included for more informative json serialization
    // todo this could be even lazy val
//    fun eventType(): String = getNameClassValueCache(this.javaClass)
    val eventType: String by lazy{ getNameClassValueCache(this.javaClass)}

    val tickTime = time.epochSeconds //todo@tt incorrect

    override fun toJson(): JSONObject = json {
        "time" to time
        "type" to eventType
    }


    // we intentionally override toStiring again to remove json line-breaks and indents
    override fun toString() = toJson().toString()
}


fun interface EventListener {
    fun consume(event: Event)
}




/**
 * Activates a global event-log, which stores all events on the kalasim event bus.
 *
 * See [Event Log](https://www.kalasim.org/events/) for details.
 *
 * @sample org.kalasim.dokka.eventsHowTo
 */
//@Deprecated("Use ", replaceWith = ReplaceWith("collect<Event>()"))
fun Environment.enableEventLog(): EventLog {
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
 * @param filter Optional filter lambda to further filter the events when populating the list
 */
inline fun <reified E : Event> Environment.collect(crossinline filter: E.() -> Boolean = { true }): List<E> {
    val traces: MutableList<E> = mutableListOf()

    addEventListener {
        if(it is E && filter(it)) traces.add(it)
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
