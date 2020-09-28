import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    application
}

group = "me.brandl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // cant upgrade to 1.8 because of https://issues.apache.org/jira/browse/CSV-257
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")

    testImplementation(kotlin("test-junit"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

//application {
//    mainClassName = "MainKt"
//}