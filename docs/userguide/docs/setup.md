# Installation

`kalasim` requires Java11 or higher.

## Gradle

To get started, simply add it as a dependency:
```
dependencies {
    implementation 'com.github.holgerbrandl:kalasim:1.1.`1`'
}
```

Builds are hosted on [maven-central](https://search.maven.org/search?q=a:kalasim) supported by the great folks at [sonatype](https://www.sonatype.com/).

## Jitpack Integration

You can also use [JitPack with Maven or Gradle](https://jitpack.io/#holgerbrandl/kalasim) to include the latest snapshot as a dependency in your project.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
        implementation 'com.github.holgerbrandl:kalasim:-SNAPSHOT'
}
```

## How to build it from sources?

To build and install it into your local maven cache, simply clone the repo and run
```bash
./gradlew install
```
