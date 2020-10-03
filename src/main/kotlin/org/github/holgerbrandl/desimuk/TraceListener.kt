package org.github.holgerbrandl.desimuk

fun interface TraceListener {
    fun processTrace(traceElement: TraceElement)
}