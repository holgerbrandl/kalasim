package org.kalasim.analysis

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.add
import org.kalasim.Environment
import org.kalasim.TickTime
import org.kalasim.toWallTime

fun DataFrame<*>.addWallTimes(sim: Environment, suffix: String = "_wt"): DataFrame<*> {
    // figure which columns are of type ticktime
    val tickTimeColumns = columns().filter { it.type().toString().contains("TickTime") }.map { it.name() }

    return tickTimeColumns.fold(this) { df, ttColumnName ->
        df.add(ttColumnName + suffix) {
            val tickTime = ttColumnName<TickTime?>()
            tickTime?.let { sim.toWallTime(it) }
        }
    }
}