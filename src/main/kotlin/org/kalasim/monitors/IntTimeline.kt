package org.kalasim.monitors

import org.kalasim.misc.DependencyContext
import org.koin.core.Koin

class IntTimeline(
    name: String? = null,
    initialValue: Int = 0,
    koin: Koin = DependencyContext.get()
) : MetricTimeline<Int>(name, initialValue, koin) {


    /** Increment the current value by 1 and add it as value. Autostart with 0 if there is no prior value. */
    operator fun inc(): IntTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue(roundToInt + 1)

        return this
    }

    operator fun dec(): IntTimeline {
//        val roundToInt = (values.lastOrNull() ?: 0.0).roundToInt()
        val roundToInt = values.last()
        addValue((roundToInt - 1))

        return this
    }
}

class DoubleTimeline(
    name: String? = null,
    initialValue: Double = 0.0,
    koin: Koin = DependencyContext.get()
) : MetricTimeline<Double>(name, initialValue, koin)