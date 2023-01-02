plugins {
    kotlin("jvm") version "1.8.0"
}

group = "org.kalasim.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    api("com.github.holgerbrandl:kalasim:0.9-SNAPSHOT")
    api("org.optaplanner:optaplanner-core:8.32.0.Final")
//    implementation("org.optaplanner:optaplanner-benchmark:8.19.0.Final")

    api("org.jetbrains.kotlinx:dataframe-excel:0.8.1")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}