package org.kalasim.animation

import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.kalasim.*
import org.kalasim.logistics.Rectangle
import org.kalasim.misc.AmbiguousDuration
import org.kalasim.misc.DependencyContext
import org.openrndr.math.Vector2
import java.awt.geom.Point2D
import java.time.format.DateTimeFormatter


fun <T : Environment> T.startSimulation(speedUp: Double = 1.0, smoothness: Int = 100) {
    require(speedUp > 0) { "speed up must be strictly positive" }

    apply {
        dependency { ClockSync(speedUp = speedUp, syncsPerTick = smoothness) }

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

fun Instant.format(format: String) = toLocalDateTime(TimeZone.UTC)
    .toJavaLocalDateTime()
    .format(DateTimeFormatter.ofPattern(format))