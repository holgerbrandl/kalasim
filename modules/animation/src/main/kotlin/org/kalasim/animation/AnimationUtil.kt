package org.kalasim.animation

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
