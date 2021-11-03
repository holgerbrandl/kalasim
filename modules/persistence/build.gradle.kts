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

    api("com.thoughtworks.xstream:xstream:1.4.15")
    api("com.esotericsoftware:kryo:5.1.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")

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
