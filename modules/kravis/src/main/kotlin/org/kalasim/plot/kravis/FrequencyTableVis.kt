package org.kalasim.plot.kravis

import kravis.GGPlot
import kravis.geomCol
import kravis.plot
import org.kalasim.monitors.FrequencyTable

fun <T> FrequencyTable<T>.display(title: String? = null): GGPlot {
    val data = toList()

    return data.plot(x = { it.first }, y = { it.second })
        .geomCol()
        .run { if(title != null) title(title) else this }
        .showOptional()
}