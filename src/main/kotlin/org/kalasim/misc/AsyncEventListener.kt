package org.kalasim.misc

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kalasim.Event
import org.kalasim.EventListener
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

class AsyncEventListener : EventListener {
    // These should be private, but the inline forces us to be public.
    val eventChannel = Channel<Event>(Channel.UNLIMITED)
    val future: AtomicReference<Future<*>> = AtomicReference()

    val isClosed: Boolean get() = future.get()?.isDone ?: false

    inline fun <reified T : Event> start(crossinline block: (event: T) -> Unit) {
        future.set(newSingleThreadExecutor().submit {
            runBlocking {
                for(event in eventChannel) {
                    println("received event ${event::class.simpleName}")
                    if(event is T)
                        block(event)
                }
            }
        })
    }

    override fun consume(event: Event) {
        runBlocking {
            launch {
                //println("adding event $event to channel")
                eventChannel.trySend(event).also { result ->
                    if(result.isFailure)
                        error("Failed to add event $event to channel")
                }
            }
        }
    }

    fun stop() {
        eventChannel.close()
    }
}