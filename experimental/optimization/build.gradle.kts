plugins {
    kotlin("jvm") version "1.9.21"
}

group = "org.kalasim.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://timefold.jfrog.io/artifactory/releases/")
    }
}

dependencies {

//    api("com.github.holgerbrandl:kalasim:0.9-SNAPSHOT")
    api("com.github.holgerbrandl:kalasim:0.12.105")

    api("ai.timefold.solver:timefold-solver-core:1.7.0")
    api("ai.timefold.solver:timefold-solver-benchmark:1.7.0")

    api("ch.qos.logback:logback-classic:1.4.12")

    implementation("com.google.code.gson:gson:2.10")
    api("org.jetbrains.kotlinx:dataframe-excel:0.12.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}