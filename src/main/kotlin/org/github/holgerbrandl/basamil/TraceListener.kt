package org.github.holgerbrandl.basamil

interface TraceListener {
    fun processTrace(traceElement: TraceElement)
}