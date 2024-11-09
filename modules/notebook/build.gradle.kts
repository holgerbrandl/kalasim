plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
    signing

    // see https://kotlinlang.slack.com/archives/C4W52CFEZ/p1641056747134600
    id("org.jetbrains.kotlin.jupyter.api") version "0.12.0-285"
}

version = "${rootProject.version}"

dependencies {
    api(project(":"))

    api(project(":modules:kravis4klsm"))
    api(project(":modules:letsplot"))


    testImplementation(kotlin("test"))
    testImplementation(project(":"))
}

tasks.test {
    useJUnitPlatform()
}


//https://github.com/Kotlin/kotlin-jupyter/blob/master/docs/libraries.md
tasks.processJupyterApiResources {
    libraryProducers = listOf("org.kalasim.analysis.NotebookIntegration")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kalasim-notebook"

            pom {
                url.set("https://www.kalasim.org")
                name.set("kalasim")
                description.set("kalasim is a process-oriented discrete event simulation engine")

                scm {
                    connection.set("scm:git:github.com/holgerbrandl/kalasim.git")
                    url.set("https://github.com/holgerbrandl/kalasim.git")
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/holgerbrandl/kalasim/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("holgerbrandl")
                        name.set("Holger Brandl")
                        email.set("holgerbrandl@gmail.com")
                    }
                }
            }
        }
    }
}


java {
    withJavadocJar()
    withSourcesJar()
}


signing {
    sign(publishing.publications["maven"])
}

kotlin {
    jvmToolchain(17)
}
