package org.kalasim.webui

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component
import java.io.File

/**
 * @author Holger Brandl
 */
@SpringBootApplication(scanBasePackages = ["org.kalasim.webui"])
open class ConfigUI

fun main(args: Array<String>) {

    SpringApplicationBuilder().sources(ConfigUI::class.java).properties(mapOf("server.port" to 8080)).run(*args)
}


@Component
class PortainerProvider {

    // https://stackoverflow.com/questions/23513045/how-to-check-if-a-process-is-running-inside-docker-container
    private val isDockerized = File("/.dockerenv").isFile

    // note this requires a volumne bind  - /etc/hostname:/etc/dockerhost
    // https://stackoverflow.com/questions/34943632/linux-check-if-there-is-an-empty-line-at-the-end-of-a-file
    val portainerHost = if (isDockerized) File("/etc/dockerhost").readText().trim() else "dddocker02"
    val portainerPort = 9000
}