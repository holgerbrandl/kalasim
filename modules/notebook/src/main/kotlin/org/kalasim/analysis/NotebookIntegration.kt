package org.kalasim.analysis

import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.kalasim.Environment
import org.kalasim.analysis.snapshot.QueueSnapshot
import org.kalasim.misc.printThis
import org.kalasim.misc.toIndentString


@Suppress("unused")
//@JupyterLibrary
internal class NotebookIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        import("org.kalasim.*")
        import(
            "kotlin.time.Duration.Companion.minutes",
            "kotlin.time.Duration.Companion.seconds",
            "kotlin.time.Duration.Companion.hours",
            "kotlin.time.Duration.Companion.days",
        )

        render<Environment> { it.toJson().toIndentString().printThis() }
        render<QueueSnapshot> { it.toJson().toIndentString().printThis() }
    }
}