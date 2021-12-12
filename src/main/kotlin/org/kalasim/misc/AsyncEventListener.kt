package org.kalasim.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kalasim.Event
import org.kalasim.EventListener

class AsyncEventListener(val scope: CoroutineScope = GlobalScope) : EventListener {
    val eventChannel = Channel<Event>()

    inline fun <reified T : Event> start(crossinline block: (event: T) -> Unit) {
        scope.launch {
            eventChannel.receiveAsFlow()
                .collect { event: Event ->
                    println("received event ${event}")
                    if (event is T)
                        block.invoke(event)
                }
        }
    }

    override fun consume(event: Event) {
        scope.launch {
//            println("adding event ${event} to channel")
            eventChannel.trySend(event)
        }
    }
}