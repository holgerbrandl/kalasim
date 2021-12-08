package org.kalasim.misc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kalasim.ComponentStateChangeEvent
import org.kalasim.EntityCreatedEvent
import org.kalasim.Event
import org.kalasim.EventListener
import org.kalasim.InteractionEvent
import org.kalasim.MetricEvent
import org.kalasim.ResourceActivityEvent
import org.kalasim.ResourceEvent

@DslMarker
annotation class KalasimDslMarker

@KalasimDslMarker
fun asyncEventListener(scope: CoroutineScope = GlobalScope, block: CallbackActions.() -> Unit): AsyncEventListener =
    CallbackActions()
        .apply(block)
        .let { callbackActions ->
            AsyncEventListener(callbackActions, scope)
        }

class AsyncEventListener(callbacks: CallbackActions, scope: CoroutineScope = GlobalScope) : EventListener {
    private val eventChannel = Channel<Event>()

    init {
        scope.launch {
            eventChannel.receiveAsFlow()
                .collect { event: Event ->
                    callbacks.anyEventActions.forEach { it.invoke(event) }
                    when (event) {
                        is ComponentStateChangeEvent -> callbacks.componentStateChangeEventActions.forEach {
                            it.invoke(event)
                        }
                        is EntityCreatedEvent -> callbacks.entityCreatedEventActions.forEach { it.invoke(event) }
                        is InteractionEvent -> callbacks.interactionEventActions.forEach { it.invoke(event) }
                        is MetricEvent -> callbacks.metricEventActions.forEach { it.invoke(event) }
                        is ResourceActivityEvent -> callbacks.resourceActivityEventActions.forEach { it.invoke(event) }
                        is ResourceEvent -> callbacks.resourceEventActions.forEach { it.invoke(event) }
                        else -> throw IllegalArgumentException("Unknown event type: $event")
                    }
                }
        }
    }

    override fun consume(event: Event) {
        GlobalScope.launch {
            eventChannel.trySend(event)
        }
    }
}

class CallbackActions {
    internal val anyEventActions by lazy { mutableListOf<(event: Event) -> Unit>() }
    internal val componentStateChangeEventActions by lazy { mutableListOf<(event: ComponentStateChangeEvent) -> Unit>() }
    internal val entityCreatedEventActions by lazy { mutableListOf<(event: EntityCreatedEvent) -> Unit>() }
    internal val interactionEventActions by lazy { mutableListOf<(event: InteractionEvent) -> Unit>() }
    internal val metricEventActions by lazy { mutableListOf<(event: MetricEvent) -> Unit>() }
    internal val resourceActivityEventActions by lazy { mutableListOf<(event: ResourceActivityEvent) -> Unit>() }
    internal val resourceEventActions by lazy { mutableListOf<(event: ResourceEvent) -> Unit>() }

    fun onAnyEvent(block: (event: Event) -> Unit) {
        anyEventActions += block
    }

    fun onComponentStateChangeEvent(block: (event: ComponentStateChangeEvent) -> Unit) {
        componentStateChangeEventActions += block
    }

    fun onEntityCreatedEvent(block: (event: EntityCreatedEvent) -> Unit) {
        entityCreatedEventActions += block
    }

    fun onInteractionEvent(block: (event: InteractionEvent) -> Unit) {
        interactionEventActions += block
    }

    fun onMetricEvent(block: (event: MetricEvent) -> Unit) {
        metricEventActions += block
    }

    fun onResourceActivityEvent(block: (event: ResourceActivityEvent) -> Unit) {
        resourceActivityEventActions += block
    }

    fun onResourceEvent(block: (event: ResourceEvent) -> Unit) {
        resourceEventActions += block
    }
}