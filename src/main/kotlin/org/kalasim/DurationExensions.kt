@file:Suppress("PackageDirectoryMismatch")

package org.kalasim

import kotlin.time.*


public inline val Int.minute get() = toDuration(DurationUnit.MINUTES)
public inline val Long.minute get() = toDuration(DurationUnit.MINUTES)

public inline val Int.hour get() = toDuration(DurationUnit.HOURS)
public inline val Long.hour get() = toDuration(DurationUnit.HOURS)

public inline val Int.day get() = toDuration(DurationUnit.DAYS)
public inline val Long.day get() = toDuration(DurationUnit.DAYS)

//typealias days = kotlin.time.Duration.Companion.

// methods below bring back fractional getters which were removed from the stdlib in 1.9

/** The value of this duration expressed as a Double number of days. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
val  Duration.inDays: Double
    get() = toDouble(DurationUnit.DAYS)

/** The value of this duration expressed as a Double number of hours. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
val  Duration.inHours: Double
    get() = toDouble(DurationUnit.HOURS)

/** The value of this duration expressed as a Double number of minutes. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
val  Duration.inMinutes: Double
    get() = toDouble(DurationUnit.MINUTES)

/** The value of this duration expressed as a Double number of seconds. */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal val Duration.inSeconds
    get() = toDouble(DurationUnit.SECONDS)

