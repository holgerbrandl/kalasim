import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.5.31"
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
//    api("com.esotericsoftware:kryo:5.2.1-SNAPSHOT")
    api("com.esotericsoftware:kryo:5.2.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    api("com.github.holgerbrandl:krangl:0.17")
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
