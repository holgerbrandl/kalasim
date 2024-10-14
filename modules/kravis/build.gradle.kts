plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
    signing
}

version = "${rootProject.version}"
group = "com.github.holgerbrandl"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":"))
//    api("com.github.holgerbrandl:kalasim:0.12.109")

    api("com.github.holgerbrandl:kravis:0.9.96")
//    api("com.github.holgerbrandl:krangl:0.18.4")

    testImplementation(kotlin("test"))
//    testImplementation(project(":"))
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kalasim-kravis"

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



signing {
    sign(publishing.publications["maven"])
}

kotlin {
    jvmToolchain(11)
}
