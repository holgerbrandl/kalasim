package org.kalasim.animation

import org.kalasim.Component
import org.kalasim.asDuration

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

    override fun repeatedProcess() = sequence {
        if(stop) {
            stopSimulation()
        }

        hold(env.asDuration(1 / rate))
    }
}