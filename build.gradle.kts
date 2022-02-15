plugins {
    kotlin("jvm") version "1.6.10"
//    kotlin("jvm") version "1.5.31"
    `maven-publish`
    signing

//    id("org.jetbrains.kotlin.jupyter.api") version "0.10.0-131-1" // "0.11.0-45"
    // see https://kotlinlang.slack.com/archives/C4W52CFEZ/p1641056747134600
    id("org.jetbrains.kotlin.jupyter.api") version "0.11.0-45" // "0.11.0-45"

    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

group = "com.github.holgerbrandl"
version = "0.7.98-SNAPSHOT"
//version = "0.7.96"


repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")
    api("io.insert-koin:koin-core:3.1.4")
    implementation(kotlin("reflect"))


    api("com.github.holgerbrandl:jsonbuilder:0.9")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    //  api("io.github.microutils:kotlin-logging:1.12.5")
//    api("org.slf4j:slf4j-simple:1.7.32")

    implementation("com.google.code.gson:gson:2.8.9")

//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotest:kotest-assertions-core:5.0.3")

    // **TODO** move to api to require users to pull it in if needed
    implementation("com.github.holgerbrandl:krangl:0.17.3")

    compileOnly("com.github.holgerbrandl:kravis:0.8.5")
    testImplementation("com.github.holgerbrandl:kravis:0.8.5")

    compileOnly("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.1.0")
    testImplementation("org.jetbrains.lets-plot:lets-plot-batik:2.2.0")
    //    testImplementation("org.jetbrains.lets-plot:lets-plot-jfx:1.5.4")

    //experimental dependencies  use for experimentation
    testImplementation("com.thoughtworks.xstream:xstream:1.4.18")

    //https://youtrack.jetbrains.com/issue/KT-44197

    testImplementation(kotlin("script-runtime"))
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
//}


//https://github.com/Kotlin/kotlin-jupyter/blob/master/docs/libraries.md
tasks.processJupyterApiResources {
    libraryProducers = listOf("org.kalasim.analysis.NotebookIntegration")
}

//https://gist.github.com/domnikl/c19c7385927a7bef7217aa036a71d807
val jar by tasks.getting(Jar::class) {
    manifest {
//        attributes["Main-Class"] = "com.example.MainKt"
        attributes["Implementation-Title"] = "kalasim"
        attributes["Implementation-Version"] = project.version
    }
}

//
//subprojects {
//    java.sourceCompatibility = JavaVersion.VERSION_1_8
//    java.targetCompatibility = JavaVersion.VERSION_1_8
//}

//application {
//    mainClassName = "MainKt"
//}

//bintray kts example https://gist.github.com/s1monw1/9bb3d817f31e22462ebdd1a567d8e78a

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


publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
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


nexusPublishing {
//    packageGroup.set("com.github.holgerbrandl.kalasim")

    repositories {
        sonatype {
//            print("staging id is ${project.properties["sonatypeStagingProfileId"]}")
            stagingProfileId.set(project.properties["sonatypeStagingProfileId"] as String?)

//            nexusUrl.set(uri("https://oss.sonatype.org/"))
//            snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))

            username.set(project.properties["ossrhUsername"] as String?) // defaults to project.properties["myNexusUsername"]
            password.set(project.properties["ossrhPassword"] as String?) // defaults to project.properties["myNexusPassword"]
        }
    }
}


signing {
    sign(publishing.publications["maven"])
}

fun findProperty(s: String) = project.findProperty(s) as String?


//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//    freeCompilerArgs = listOf("-Xinline-classes")
//}
