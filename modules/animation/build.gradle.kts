import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

group = "com.github.holgerbrandl"
//version = "1.0-SNAPSHOT"
version = "${rootProject.version}"

val orxFeatures = setOf(
    "orx-compositor",
    "orx-fx",
    "orx-gui",
    "orx-color",
//    "orx-image-fit",
//    "orx-keyframer",
//    "orx-noise",
//    "orx-olive",
    "orx-panel",
//    "orx-shade-styles",
    null
).filterNotNull()

val openrndrUseSnapshot = false
val openrndrVersion = if (openrndrUseSnapshot) "0.5.1-SNAPSHOT" else "0.4.1"

val openrndrFeatures = setOf(
    "video"
)

fun openrndr(module: String): Any {
    return "org.openrndr:openrndr-$module:$openrndrVersion"
}
fun openrndrNatives(module: String): Any {
    return "org.openrndr:openrndr-$module-natives-windows:$openrndrVersion"
}
fun orx(module: String): Any {
    return "org.openrndr.extra:$module:$openrndrVersion"
}


repositories {
    mavenCentral()
//    mavenLocal()
    maven(url = "https://maven.openrndr.org")
}

dependencies {
    api(project(":"))

//    implementation(project ("kalasim"))
//    api("com.github.holgerbrandl:kalasim:0.7.93-SNAPSHOT")

//    api("com.github.holgerbrandl:kravis:0.8.5")

    runtimeOnly(openrndr("gl3"))
    runtimeOnly(openrndrNatives("gl3"))
//    implementation(openrndr("openal"))
//    runtimeOnly(openrndrNatives("openal"))
    api(openrndr("core"))
    api(openrndr("color"))
    api(openrndr("svg"))
//    implementation(openrndr("animatable"))
//    implementation(openrndr("extensions"))
//    implementation(openrndr("filter"))

//    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core","1.6.0")
    implementation("io.github.microutils", "kotlin-logging-jvm","2.0.6")
//    implementation(kotlin("script-runtime"))

    if ("video" in openrndrFeatures) {
        implementation(openrndr("ffmpeg"))
        runtimeOnly(openrndrNatives("ffmpeg"))
    }

    for (feature in orxFeatures) {
        api(orx(feature))
    }

    testImplementation(kotlin("test"))
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

