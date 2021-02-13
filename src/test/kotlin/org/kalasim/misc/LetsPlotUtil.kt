package org.kalasim.misc

import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.plot.MonolithicAwt
import jetbrains.datalore.vis.svg.SvgSvgElement
import jetbrains.datalore.vis.swing.BatikMapperComponent
import jetbrains.datalore.vis.swing.BatikMessageCallback
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.intern.toSpec
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

//https://github.com/JetBrains/lets-plot-kotlin/blob/master/demo/jvm-batik/src/main/kotlin/minimalDemo/Main.kt#L61
fun Plot.showPlot() {

// Setup

    val BATIK_MESSAGE_CALLBACK = object : BatikMessageCallback {
        override fun handleMessage(message: String) {
            println(message)
        }

        override fun handleException(e: Exception) {
            if(e is RuntimeException) {
                throw e
            }
            throw RuntimeException(e)
        }
    }

    val SVG_COMPONENT_FACTORY_BATIK =
        { svg: SvgSvgElement -> BatikMapperComponent(svg, BATIK_MESSAGE_CALLBACK) }

    val AWT_EDT_EXECUTOR = { runnable: () -> Unit ->
        // Just invoke in the current thread.
        runnable.invoke()
    }

    SwingUtilities.invokeLater {

        // Create Swing Panel showing the plot.
        val plotSpec = toSpec()
        val plotSize = DoubleVector(1200.0, 600.0)

        val component =
            MonolithicAwt.buildPlotFromRawSpecs(
                plotSpec,
                plotSize,
//                    plotMaxWidth = null,
                SVG_COMPONENT_FACTORY_BATIK, AWT_EDT_EXECUTOR
            ) {
                for(message in it) {
                    println("PLOT MESSAGE: $message")
                }
            }

        // Show plot in Swing frame.
        val frame = JFrame("The Minimal")
        frame.contentPane.add(component)
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.pack()
        frame.isVisible = true
    }
}