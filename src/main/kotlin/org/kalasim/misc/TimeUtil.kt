package org.kalasim.misc.time

import kotlin.time.Duration


// Duration utilities
fun Collection<Duration>.sum() : Duration = sumOf { it }

fun <T> Iterable<T>.sumOf(selector: (T) -> Duration) = map{ selector(it)}.sum()
//fun <T> Iterable<Duration>.sum() = reduce{d1, d2 -> d1+d2}
