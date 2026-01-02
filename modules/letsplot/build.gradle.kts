plugins {
    kotlin("jvm") version "2.2.21"
    `maven-publish`
    signing
}

version = "${rootProject.version}"

//repositories {
//    mavenCentral()
//    mavenLocal()
//}

dependencies {
    api(project(":"))

    api("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.7.0")

    testImplementation("org.jetbrains.lets-plot:lets-plot-batik:4.3.3")

    testImplementation(kotlin("test"))
    testImplementation(project(":"))
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kalasim-letsplot"

            pom {
                url.set("https://www.kalasim.org")
                name.set("kalasim")
                description.set("kalasim is a process-oriented discrete event simulation engine")

                scm {
                    connection.set("scm:git:github.com/holgerbrandl/kalasim.git")
                    url.set("https://github.com/holgerbrandl/kalasim.git")
                }

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/holgerbrandl/kalasim/master/LICENSE")
                    }
                }

                developers {
                    developer {
                        id.set("holgerbrandl")
                        name.set("Holger Brandl")
                        email.set("holgerbrandl@gmail.com")
                    }
                }
            }
        }
    }
}


java {
    withJavadocJar()
    withSourcesJar()
}



signing {
    sign(publishing.publications["maven"])
}

kotlin {
    jvmToolchain(21)
}
