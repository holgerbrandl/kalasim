import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.20"
    `maven-publish`
    signing

    // see https://kotlinlang.slack.com/archives/C4W52CFEZ/p1641056747134600
    id("org.jetbrains.kotlin.jupyter.api") version "0.12.0-285"

    id("io.github.gradle-nexus.publish-plugin") version "1.2.0"
}

group = "com.github.holgerbrandl"
version = "0.12.110-SNAPSHOT"


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")
    // note updated postponed because of regression errors
    api("io.insert-koin:koin-core:3.1.2")

    api("org.json:json:20240303")
    api("com.github.holgerbrandl:jsonbuilder:0.10")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    api("io.github.oshai:kotlin-logging-jvm:6.0.9")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.20")

//    implementation("org.jetbrains.kotlinx:dataframe:0.13.1")

    implementation("com.github.holgerbrandl:kravis:0.9.96")

    // **TODO** move to api to require users to pull it in if needed
    implementation("com.github.holgerbrandl:krangl:0.18.4") // must needed for kravis
    implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.7.0") // override old version in krangl

    implementation("com.github.holgerbrandl:kdfutils:1.3.5")

    compileOnly("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.7.0")


    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("com.github.holgerbrandl:kdfutils:1.3.5")
    testImplementation("com.github.holgerbrandl:kravis:0.9.96")

    testImplementation("org.jetbrains.lets-plot:lets-plot-batik:4.3.3")
    //    testImplementation("org.jetbrains.lets-plot:lets-plot-jfx:1.5.4")

    //experimental dependencies  use for experimentation
    testImplementation("com.thoughtworks.xstream:xstream:1.4.20")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")

    testImplementation(kotlin("script-runtime"))
    testImplementation(kotlin("test"))
}



tasks.test {
    useJUnitPlatform()
}

//https://github.com/Kotlin/kotlin-jupyter/blob/master/docs/libraries.md
tasks.processJupyterApiResources {
    libraryProducers = listOf("org.kalasim.analysis.NotebookIntegration")
}

//https://gist.github.com/domnikl/c19c7385927a7bef7217aa036a71d807
val jar by tasks.getting(Jar::class) {
    manifest {
//        attributes["Main-Class"] = "com.example.MainKt"
        attributes["Implementation-Title"] = "kalasim"
        attributes["Implementation-Version"] = project.version
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}


// see https://youtrack.jetbrains.com/issue/KT-52735
val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    freeCompilerArgs.addAll(listOf("-Xallow-any-scripts-in-source-roots"))
//    freeCompilerArgs.addAll(listOf("-Xcontext-receivers"))
}


kotlin {
    jvmToolchain(11)
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

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


nexusPublishing {
//    packageGroup.set("com.github.holgerbrandl.kalasim")
    repositories {
        sonatype()
    }
}


signing {
    sign(publishing.publications["maven"])
}

fun findProperty(s: String) = project.findProperty(s) as String?
