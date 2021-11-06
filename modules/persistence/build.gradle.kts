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

    api("com.github.holgerbrandl:kalasim:0.6.91")
    api("com.github.holgerbrandl:kalasim:0.7-SNAPSHOT")
    api("de.ruedigermoeller:fst:2.56")


    api("com.thoughtworks.xstream:xstream:1.4.18")
    api("com.esotericsoftware:kryo:5.2.0")
    api("com.squareup.moshi:moshi-kotlin:1.12.0")
    api("com.google.code.gson:gson:2.8.8")


    api("com.github.holgerbrandl:krangl:0.17")

    api("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.0.2")

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
