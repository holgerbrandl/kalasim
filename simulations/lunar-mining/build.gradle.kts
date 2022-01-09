import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm")  version "1.6.10"
    `maven-publish`
}

group = "org.kalasim.demo"
version = "1.0-SNAPSHOT"

val orxFeatures = setOf(
    "orx-compositor",
    "orx-fx",
    "orx-gui",
    "orx-image-fit",
    "orx-keyframer",
    "orx-noise",
    "orx-olive",
    "orx-panel",
    "orx-shade-styles",
    null
).filterNotNull()

val openrndrUseSnapshot = false
val openrndrVersion = if (openrndrUseSnapshot) "0.4.0-SNAPSHOT" else "0.3.58"

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
    mavenLocal()
    maven(url = "https://maven.openrndr.org")
}

dependencies {
//    implementation(project ("kalasim"))
    api("com.github.holgerbrandl:kalasim:0.7.93-SNAPSHOT")

    api("com.github.holgerbrandl:kravis:0.8.3")
    api("com.github.holgerbrandl:krangl:0.17.2")

    runtimeOnly(openrndr("gl3"))
    runtimeOnly(openrndrNatives("gl3"))
    implementation(openrndr("openal"))
    runtimeOnly(openrndrNatives("openal"))
    implementation(openrndr("core"))
    implementation(openrndr("svg"))
    implementation(openrndr("animatable"))
    implementation(openrndr("extensions"))
    implementation(openrndr("filter"))

    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core","1.5.0-RC")
    implementation("io.github.microutils", "kotlin-logging-jvm","2.0.6")
    implementation(kotlin("script-runtime"))

    if ("video" in openrndrFeatures) {
        implementation(openrndr("ffmpeg"))
        runtimeOnly(openrndrNatives("ffmpeg"))
    }

    for (feature in orxFeatures) {
        implementation(orx(feature))
    }

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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

