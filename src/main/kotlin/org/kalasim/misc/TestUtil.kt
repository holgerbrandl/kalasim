package org.kalasim.misc

import kotlinx.datetime.Instant
import org.kalasim.*
import kotlin.time.DurationUnit

class TestUtil {

    companion object {
        fun requests(component: Component) = component.requests
        fun claims(component: Component) = component.claims
    }

}

fun createTestSimulation(
    startDate: Instant = Instant.fromEpochMilliseconds(0),
    enableComponentLogger: Boolean = true,
    /** The duration unit of this environment. Every tick corresponds to a unit duration. See https://www.kalasim.org/basics/#running-a-simulation */
    tickDurationUnit: DurationUnit = DurationUnit.MINUTES,
    builder: Environment.() -> Unit,
) {
    createSimulation(startDate, tickDurationUnit = tickDurationUnit) {
        if(enableComponentLogger) enableComponentLogger()
        builder()
    }
}

internal fun <S : Environment> testModel(sim: S, smthg: S.() -> Unit): Unit = smthg(sim)