import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

//val kotlinVersion = KotlinVersion.CURRENT

plugins {
    kotlin("jvm") version "1.5.31"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
//    kotlin("kapt") version "1.5.31"
}


//group = "org.kalasim"
group = "com.github.holgerbrandl"
version = "0.7-SNAPSHOT"
//version = "0.6.91"


repositories {
    mavenCentral()
//    jcenter() // still needed because of lets-plot
//    mavenLocal()
}

dependencies {
//    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")

    api("org.apache.commons:commons-math3:3.6.1")

    //cant upgrade because of https://github.com/InsertKoinIO/koin/issues/939
//    implementation("org.koin:koin-core:2.1.6")
    api("io.insert-koin:koin-core:3.1.3")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")

    api("com.github.holgerbrandl:jsonbuilder:0.9")
    implementation("com.google.code.gson:gson:2.8.8")
    api("com.squareup.moshi:moshi-kotlin:1.12.0")


//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")

    implementation("com.github.holgerbrandl:krangl:0.17")

    compileOnly("com.github.holgerbrandl:kravis:0.8.1")
    testImplementation("com.github.holgerbrandl:kravis:0.8.1")

    compileOnly("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:3.0.2")
    testImplementation("org.jetbrains.lets-plot:lets-plot-batik:2.2.0")
    //    testImplementation("org.jetbrains.lets-plot:lets-plot-jfx:1.5.4")

    //experimental dependencies  use for experimentation
    testImplementation("com.thoughtworks.xstream:xstream:1.4.18")

    //https://youtrack.jetbrains.com/issue/KT-44197

    testImplementation(kotlin("script-runtime"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
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