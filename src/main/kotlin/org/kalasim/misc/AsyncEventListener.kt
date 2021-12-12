package org.kalasim.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kalasim.Event
import org.kalasim.EventListener

class AsyncEventListener(val scope: CoroutineScope = GlobalScope) : EventListener {
    val eventChannel = Channel<Event>()

    inline fun <reified T : Event> start(crossinline block: (event: T) -> Unit) {
        scope.launch {
            eventChannel
                .receiveAsFlow()
                //.onEach { println("received event ${it::class.simpleName}") }
                .filter { it is T }
                .collect { event: Event -> block.invoke(event as T) }
        }
    }

    override fun consume(event: Event) {
        runBlocking {
            launch {
                println("adding event $event to channel")
                eventChannel.trySend(event)
                    .also { result ->
                        println("result: $result")
                        if (result.isFailure)
                            println("Failed to add event $event to channel")
                    }
            }
        }
    }
}