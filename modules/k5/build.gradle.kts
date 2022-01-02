plugins {
    kotlin("jvm")
//    `maven-publish`
//    signing
//    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

group = "org.kalasim.json"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//    mavenLocal()
}

dependencies {
    //    implementation("com.github.holgerbrandl:kalasim:0.7-SNAPSHOT")
    implementation(project(":"))

    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
    implementation("com.google.code.gson:gson:2.8.9")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}