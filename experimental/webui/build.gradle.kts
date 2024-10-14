import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"

}

group = "org.kalasim.webui"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))

    // webui
//    implementation ("org.springframework:spring-websocket:5.2.5.RELEASE")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:2.5.1")

    // just needed to enable hotswap resource reloading
    // see https://docs.spring.io/spring-boot/docs/1.5.16.RELEASE/reference/html/howto-hotswapping.html
    implementation("org.springframework.boot:spring-boot-devtools:2.5.1")
    implementation ("org.springframework.boot:spring-boot-starter-web:2.5.1")
    implementation("com.github.holgerbrandl:kalasim:0.7-SNAPSHOT")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}