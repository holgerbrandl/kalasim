import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.5.21"
    `maven-publish`
}

group = "org.kalasim.examples"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    api("com.github.holgerbrandl:kalasim:0.7-SNAPSHOT")
    api("com.github.holgerbrandl:kravis:0.8.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
