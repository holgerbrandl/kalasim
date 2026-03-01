package org.kalasim.plot.kravis

import kotlinx.datetime.Instant
import org.kalasim.SimTime
import kotlin.time.DurationUnit

fun interface TimeDiscretizer {
    fun discretize(time: SimTime): SimTime

    companion object {
        val DAY = roundTo(DurationUnit.DAYS)
        val HOUR = roundTo(DurationUnit.HOURS)
        val MINUTE = roundTo(DurationUnit.MINUTES)
        val SECOND = roundTo(DurationUnit.SECONDS)
        val MILLISECOND = roundTo(DurationUnit.MILLISECONDS)
        val MICROSECOND = roundTo(DurationUnit.MICROSECONDS)
        val NANOSECOND = roundTo(DurationUnit.NANOSECONDS)
    }
}

enum class RoundingMode {
    UP, DOWN, NATURAL
}

/**
 * Creates a TimeDiscretizer that rounds Instant to the specified DurationUnit.
 * 
 * @param unit The DurationUnit to round to (e.g., DurationUnit.HOURS, DurationUnit.MINUTES)
 * @param mode The rounding mode to use (UP, DOWN, or NATURAL). Defaults to NATURAL.
 * @return A TimeDiscretizer function that rounds the input SimTime
 */
fun roundTo(unit: DurationUnit, mode: RoundingMode = RoundingMode.DOWN): TimeDiscretizer =
    TimeDiscretizer { instant ->
        val epochSeconds = instant.epochSeconds
        val nanos = instant.nanosecondsOfSecond
        val totalSeconds = epochSeconds + nanos / 1_000_000_000.0

        val unitInSeconds = when (unit) {
            DurationUnit.DAYS -> 86400.0
            DurationUnit.HOURS -> 3600.0
            DurationUnit.MINUTES -> 60.0
            DurationUnit.SECONDS -> 1.0
            DurationUnit.MILLISECONDS -> 0.001
            DurationUnit.MICROSECONDS -> 0.000001
            DurationUnit.NANOSECONDS -> 0.000000001
        }

        val valueInUnits = totalSeconds / unitInSeconds
        val roundedValue = when (mode) {
            RoundingMode.UP -> kotlin.math.ceil(valueInUnits)
            RoundingMode.DOWN -> kotlin.math.floor(valueInUnits)
            RoundingMode.NATURAL -> kotlin.math.round(valueInUnits)
        }

        @Suppress("DEPRECATION")
        Instant.fromEpochSeconds((roundedValue * unitInSeconds).toLong())
    }

fun TimeDiscretizer(
    unit: DurationUnit = DurationUnit.SECONDS,
    mode: RoundingMode = RoundingMode.NATURAL
): TimeDiscretizer =
    roundTo(unit, mode)