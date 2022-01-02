package org.kalasim.analysis

import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.kalasim.Environment
import org.kalasim.QueueInfo
import org.kalasim.misc.printThis
import org.kalasim.misc.toIndentString


@Suppress("unused")
//@JupyterLibrary
internal class NotebookIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        import("org.kalasim.*")

        render<Environment> { it.toJson().toIndentString().printThis() }
        render<QueueInfo> { it.toJson().toIndentString().printThis() }
    }
}