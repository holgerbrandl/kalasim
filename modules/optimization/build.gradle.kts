plugins {
    kotlin("jvm") version "1.8.10"
}

group = "org.kalasim.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

//    api("com.github.holgerbrandl:kalasim:0.9-SNAPSHOT")
    api("com.github.holgerbrandl:kalasim:0.10")
    api("org.optaplanner:optaplanner-core:8.36.0.Final")
    api("org.optaplanner:optaplanner-benchmark:8.36.0.Final")

    api("ch.qos.logback:logback-classic:1.4.5")

    implementation("com.google.code.gson:gson:2.10")

    api("org.jetbrains.kotlinx:dataframe-excel:0.8.1")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}