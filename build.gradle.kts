import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    `maven-publish`
    signing

    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

version = "1.0.5-SNAPSHOT"


allprojects {
    group = "com.github.holgerbrandl"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")

    // note update to latest version postponed because of regression errors
    api("io.insert-koin:koin-core:3.1.2")

    api("org.json:json:20240303")
    api("com.github.holgerbrandl:jsonbuilder:0.10")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    api("io.github.oshai:kotlin-logging-jvm:6.0.9")

    api("com.github.holgerbrandl:kdfutils:1.5.0")

    implementation("com.google.code.gson:gson:2.10.1")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.20")

    testImplementation("io.kotest:kotest-assertions-core:5.7.2")

    //experimental dependencies  use for experimentation
    testImplementation("com.thoughtworks.xstream:xstream:1.4.20")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")

    testImplementation(kotlin("script-runtime"))
    testImplementation(kotlin("test"))
}



tasks.test {
    useJUnitPlatform()
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
    jvmToolchain(17)
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
