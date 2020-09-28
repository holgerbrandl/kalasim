package org.github.holgerbrandl.desimuk

interface TraceListener {
    fun processTrace(traceElement: TraceElement)
}