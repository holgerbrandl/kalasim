import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

group = "com.github.holgerbrandl"
//version = "1.0-SNAPSHOT"
version = "${rootProject.version}"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api(project(":"))

    api ("org.jetbrains.kotlinx:dataframe:0.12.0")
    api("com.github.holgerbrandl:kdfutils:1.3.3")

    implementation("org.jgrapht:jgrapht-core:1.5.2")

    api("com.github.holgerbrandl:kdfutils:1.3.5")

    // todo@2023.1 disable for release
    implementation("org.slf4j:slf4j-simple:1.7.30")


    testImplementation(kotlin("test"))
    testImplementation(project(":"))
}

tasks.test {
    useJUnit()
}


//https://gist.github.com/domnikl/c19c7385927a7bef7217aa036a71d807
val jar by tasks.getting(Jar::class) {
    manifest {
//        attributes["Main-Class"] = "com.example.MainKt"
        attributes["Implementation-Title"] = "kalasim"
        attributes["Implementation-Version"] = project.version
    }
}


java {
    withJavadocJar()
    withSourcesJar()
}



// disabled because docs examples were moved back into tests
//java {
//    sourceSets["test"].java {
//        srcDir("docs/userguide/examples/kotlin")
//    }
//}




//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//            artifactId = "kalasim-animation"
//
//        }
//    }
//}



publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "kalasim-animation"

//          artifact sourcesJar { classifier "sources" }
//          artifact javadocJar

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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

