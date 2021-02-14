# Covid19 

A kalasim simulation to simulate contact, infections and spread of the virus.


## References

https://www.youtube.com/watch?v=dW6df7SdX-4



## Gradle tasks
 - `run` runs the TemplateProgram
 - `jar` creates an executable platform specific jar file with all dependencies
 - `zipDistribution` creates a zip file containing the application jar and the data folder
 - `jpackageZip` creates a zip with a stand-alone executable for the current platform (works with Java 14 only)

## Cross builds

To create runnable jars for a platform different from the platform you use to build one uses `./gradlew jar --PtargetPlatform=<platform>`. The supported platforms are `windows`, `macos`, `linux-x64` and `linux-arm64`. Note that the `linux-arm64` platform will only work with OPENRNDR snapshot builds from master and OPENRNDR 0.3.39 (a future version).
