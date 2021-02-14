# OPENRNDR template project
A feature rich template for creating OPENRNDR programs based on Gradle/Kts

The template consists of a configuration for Gradle and an example OPENRNDR program. The Gradle configuration should serve as the
go-to starting point for writing OPENRNDR-based software.

If you are looking at this from IntelliJ IDEA you can start by expanding the _project_ tab on the left. You will find a template program in `src/main/kotlin/TemplateProgram.kt`

You will find some [basic instructions](https://guide.openrndr.org/#/02_Getting_Started_with_OPENRNDR/C00_SetupYourFirstProgram) in the [OPENRNDR guide](https://guide.openrndr.org)

## Gradle tasks
 - `run` runs the TemplateProgram
 - `jar` creates an executable platform specific jar file with all dependencies
 - `zipDistribution` creates a zip file containing the application jar and the data folder
 - `jpackageZip` creates a zip with a stand-alone executable for the current platform (works with Java 14 only)

## Cross builds
To create runnable jars for a platform different from the platform you use to build one uses `./gradlew jar --PtargetPlatform=<platform>`. The supported platforms are `windows`, `macos`, `linux-x64` and `linux-arm64`. Note that the `linux-arm64` platform will only work with OPENRNDR snapshot builds from master and OPENRNDR 0.3.39 (a future version).

## Github Actions

This repository contains a number of Github Actions in `./github/workflows`. 
The actions enable a basic build run on commit, plus publication actions that are executed when
a commit is tagged with a version number like `v0.*` or `v1.*`.