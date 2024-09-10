plugins {
    kotlin("jvm") version "1.6.10"
    `maven-publish`
}

group = "org.kalasim.serialization"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("com.github.holgerbrandl:kalasim:0.7.97-SNAPSHOT")
    api("de.ruedigermoeller:fst:3.0.3")

    api("com.thoughtworks.xstream:xstream:1.4.18")
    api("com.esotericsoftware:kryo:5.2.1")
    api("com.squareup.moshi:moshi-kotlin:1.13.0")
    api("com.github.holgerbrandl:krangl:0.17.2")

    api("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.1.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
