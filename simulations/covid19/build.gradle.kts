import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application
}

group = "org.kalasim.simulations.covid19"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.kalasim:kalasim:0.4.3")

    implementation( "com.github.holgerbrandl:kravis:0.6.1")

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "MainKt"
}