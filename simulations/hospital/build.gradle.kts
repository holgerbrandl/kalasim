plugins {
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "org.kalasim.examples.er"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("com.github.holgerbrandl:kalasim-animation:0.7.97-SNAPSHOT")
    api("com.github.holgerbrandl:kravis:0.8.5")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
