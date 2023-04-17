@file:Suppress("PackageDirectoryMismatch")

package org.kalasim.misc.time

import org.jetbrains.kotlinx.dataframe.math.mean
import org.jetbrains.kotlinx.dataframe.math.median
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


// Duration utilities
fun Iterable<Duration>.sum() : Duration = sumOf { it }
fun <T> Iterable<T>.sumOf(selector: (T) -> Duration) = map{ selector(it).inWholeMilliseconds }.sum().milliseconds

fun Iterable<Duration>.mean() : Duration = meanOf { it }
fun <T> Iterable<T>.meanOf(selector: (T) -> Duration) = map{ selector(it).inWholeMilliseconds}.mean().milliseconds

fun Iterable<Duration>.median() : Duration = meanOf { it }
fun <T> Iterable<T>.medianOf(selector: (T) -> Duration) = map{ selector(it).inWholeMilliseconds}.median().milliseconds
