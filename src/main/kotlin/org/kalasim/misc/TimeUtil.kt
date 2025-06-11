@file:Suppress("PackageDirectoryMismatch")

package org.kalasim.misc.time

import kotlin.time.Duration


// Duration utilities
/** Compute the sum of a set of durations.*/
fun Iterable<Duration>.sum(): Duration = sumOf { it }
fun List<Duration>.sum(): Duration = sumOf { it }


/** Compute the sum of a set of durations via a selector function.*/
fun <T> Iterable<T>.sumOf(selector: (T) -> Duration) = map { selector(it) }.run {
    if(isEmpty()) {
        Duration.ZERO
    } else {
        reduce { a, b -> a + b }
    }
}


/** Compute the mean of a set of durations.*/
fun Iterable<Duration>.mean(): Duration = meanOf { it }

/** Compute the mean of a set of durations via a selector function.*/
fun <T> Iterable<T>.meanOf(selector: (T) -> Duration) = map { selector(it) }.run {
    reduce { a, b -> a + b } / size
}

/** Compute the median of a set of durations.*/
fun Iterable<Duration>.median(): Duration = meanOf { it }

/** Compute the median of a set of durations via a selector function.*/
fun <T> Iterable<T>.medianOf(selector: (T) -> Duration): Duration? = map { selector(it) }.run {
    if(isEmpty()) {
        return null
    }

    val sortedDurations = sorted()
    return if(size % 2 == 0) {
        // Even list size: median is average of two middle numbers
        val midIndex = size / 2
        (sortedDurations[midIndex - 1] + sortedDurations[midIndex]) / 2
    } else {
        // Odd list size: median is the middle number
        sortedDurations[size / 2]
    }
}

// in stdlib, there are arithmetic ops for int and double but not for number
operator fun Number.times(duration: Duration) = duration.times(this.toDouble())
