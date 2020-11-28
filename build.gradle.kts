import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//val kotlinVersion = KotlinVersion.CURRENT

plugins {
    kotlin("jvm") version "1.4.10"
    application
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"

    kotlin("plugin.serialization") version "1.4.10"
}

group = "com.github.holgerbrandl"
version = "0.2-SNAPSHOT"

application {
    mainClassName = "foo.Bar" // not needed technically but makes gradle happy
}


repositories {
    mavenCentral()
    jcenter()

}

dependencies {
    // cant upgrade to 1.8 because of https://issues.apache.org/jira/browse/CSV-257
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("com.github.holgerbrandl:jsonbuilder:0.7")
    implementation("com.google.code.gson:gson:2.8.6")

    //cant upgrade because of https://github.com/InsertKoinIO/koin/issues/939
    implementation("org.koin:koin-core:2.1.6")
//    implementation("org.koin:koin-core:2.2.0")

//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotest:kotest-assertions-core:4.2.6")

    testImplementation( "com.github.holgerbrandl:kravis:0.5.2")

    //experimental dependencies  use for experimentation
    testImplementation( "com.thoughtworks.xstream:xstream:1.4.14")

    testImplementation(kotlin("script-runtime"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

//application {
//    mainClassName = "MainKt"
//}

//bintray kts example https://gist.github.com/s1monw1/9bb3d817f31e22462ebdd1a567d8e78a

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
//                    artifact sourcesJar { classifier "sources" }
//            artifact javadocJar
        }
    }
}


fun findProperty(s: String) = project.findProperty(s) as String?

bintray {
    user = findProperty("bintray_user")
    key = findProperty("bintray_key")

    publish = true
//    dryRun = false
    setPublications("maven")


    pkg(closureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "github"
        name = "kalasim"
        websiteUrl = "https://github.com/holgerbrandl/kalasim"
//        description = "Simple Lib for TLS/SSL socket handling written in Kotlin"
//        setLabels("kotlin")
        setLicenses("MIT")
        publicDownloadNumbers = true

        desc = description

        version = VersionConfig().apply{
            name  = project.name
            description = "."
            vcsTag = "v" + project.version
//            released = java.util.Date().toString()
        }

//        version{
//            name = project.version //Bintray logical version name
//                desc = '.'
//                released = new Date()
//                vcsTag = 'v' + project.version
//        }
//        versions{
//
//        }
    })
}
