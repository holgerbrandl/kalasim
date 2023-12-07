package org.kalasim.animation

import kotlinx.coroutines.*
import org.kalasim.*
import org.kalasim.logistics.Rectangle
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.DependencyContext
import org.openrndr.math.Vector2
import java.awt.geom.Point2D
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


fun <T : Environment> T.startSimulation(tickDuration: Duration = 50.milliseconds, smoothness: Int = 10) {
    apply {
        ClockSync(tickDuration = tickDuration, syncsPerTick = smoothness)

        dependency { AsyncAnimationStop() }

        CoroutineScope(Dispatchers.Default).launch {
            DependencyContext.setKoin(getKoin())
            run()
        }
    }
}

fun <K, V> Map<K, V>.cmeAvoidingCopy(): Map<K, V> {
    while(true) {
        try {
            return toMap()
        } catch(_: ConcurrentModificationException) {
        }
    }
}

fun <K> List<K>.cmeAvoidingCopy(): List<K> {
    while(true) {
        try {
            return toList()
        } catch(_: ConcurrentModificationException) {
        }
    }
}


fun <K, V> Map<K, V>.asyncCopy(): Map<K, V> {
    while(true) {
        try {
            return toMap()
        } catch(_: ConcurrentModificationException) {
        }
    }
}

fun <T> Set<T>.asyncCopy(): Set<T> {
    while(true) {
        try {
            return toSet()
        } catch(_: ConcurrentModificationException) {
        }
    }
}


fun <T> List<T>.asyncCopy(): List<T> {
    while(true) {
        try {
            return toMutableList()
        } catch(_: ConcurrentModificationException) {
        }
    }
}

fun <T> cmeGuard(function: () -> List<T>): List<T> {
    while(true) {
        try {
            return function().toMutableList()
        } catch(_: ConcurrentModificationException) {
        } catch(_: NoSuchElementException) {
        }
    }
}


class AsyncAnimationStop(val rate: Double = 1.0) : Component() {
    private var stop = false

    fun stop() {
        stop = true
    }

    @OptIn(AmbiguousDuration::class)
    override fun repeatedProcess() = sequence {
        if(stop) {
            stopSimulation()
        }

        hold(env.asDuration(1 / rate))
    }
}

fun Point2D.Double.toOpenRendrVector2(): Vector2 = Vector2(x, y)
fun Rectangle.toOpenRendrRectangle(): org.openrndr.shape.Rectangle {
    return org.openrndr.shape.Rectangle(x, y, width, height)
}