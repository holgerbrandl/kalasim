package org.kalasim.misc

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import org.kalasim.Event
import org.kalasim.EventListener

class AsyncEventListener(val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) : EventListener {
    val eventChannel = Channel<Event>(Channel.UNLIMITED)

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
//        runBlocking {
//            scope.launch {
        eventChannel.trySend(event).also { result ->
            if(result.isFailure) throw RuntimeException("Failed to add event $event to channel")
        }
//            }
//        }
    }

    fun stop() {
        eventChannel.close()
    }
}