package org.kalasim.analysis

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.rename
import org.kalasim.*

fun DataFrame<*>.addTickTimes(sim: Environment, suffix: String = "_tt"): DataFrame<*> {
    // figure which columns are of type ticktime
    val tickTimeColumns = columns().filter { it.type().toString().contains("Instant") }.map { it.name() }

    return tickTimeColumns.fold(this) { df, ttColumnName ->
        df.add(ttColumnName + suffix) {
            val simTime = ttColumnName<SimTime?>()
            simTime?.let { sim.asTickTime(it) }
        }
    }
}

/** rename *_wt columns to * and, <TickTime>_tt to <TickTime> to mimic old kalasim naming */
@Deprecated("do do this if possible but rather rework the downstream analysis scripts")
fun DataFrame<*>.toggleWtNames(suffix: String = "_tt", wt_suffix :String="_wt"): DataFrame<*> {
    val simTimeColumns = columns().filter { it.type().toString().contains("Instant") }.map { it.name() }

    val renamed = simTimeColumns.fold(this) { df, name -> df.rename(name to name + wt_suffix) }
    val tickTimeColumns = columns().filter { it.type().toString().contains("TickTime") }.map { it.name() }

    return tickTimeColumns.filter { it.endsWith(suffix) }
        .fold(renamed) { df, name -> df.rename(name to name + "") }
}
