import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.kalasim.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.jumpco.open:kfsm-jvm:1.4.31")
//    implementation "com.github.holgerbrandl:kalasim:0.6.8"
    implementation("com.github.holgerbrandl:kalasim:0.7.97")


    implementation("com.github.holgerbrandl:kravis:0.8.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}